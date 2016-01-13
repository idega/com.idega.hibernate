package com.idega.hibernate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.collection.internal.PersistentList;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.ejb.HibernateQuery;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.SessionImpl;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.cache.IWCacheManager2;
import com.idega.idegaweb.IWMainApplication;
import com.idega.util.DBUtil;

@SuppressWarnings("deprecation")
@Transactional(readOnly = true)
public class HibernateUtil extends DBUtil {

	private static final Logger LOGGER = Logger.getLogger(HibernateUtil.class.getName());

	@Override
	@SuppressWarnings("unchecked")
	public <T> T initializeAndUnproxy(T entity) {
		if (entity == null) {
			throw new NullPointerException("Entity passed for initialization is null");
		}

		Hibernate.initialize(entity);
		if (entity instanceof HibernateProxy) {
			entity = (T) ((HibernateProxy) entity).getHibernateLazyInitializer().getImplementation();
		}

		return entity;
	}

	@Override
	public boolean isInitialized(Object object) {
		return Hibernate.isInitialized(object);
	}

	@Override
	public <T> T lazyLoad(T entity) {
		if (isInitialized(entity)) {
			return entity;
		}

		boolean closeSession = false, closeTransaction = false;
		boolean canInitialize = true;
		org.hibernate.Session s = null;
		Transaction transaction = null;
		try {
			SessionFactory sessionFactory = SessionFactoryHelper.getInstance().getSessionFactory();
			s = sessionFactory.getCurrentSession();
			if (!(s instanceof SessionImpl) || !s.isOpen()) {
				closeSession = true;
				s = sessionFactory.openSession();
			}

			if (entity instanceof HibernateProxy) {
				transaction = s.getTransaction();
				if (transaction == null || !transaction.isActive()) {
					closeTransaction = true;
					transaction = s.beginTransaction();
				}
				if (!s.isConnected()) {
					LOGGER.warning("Session is not opened, can not lazy load " + entity.getClass().getName());
				}

				try {
					s.refresh(entity);
				} catch (ClassCastException e) {
					LOGGER.log(Level.WARNING, "Error refreshing entity " + entity.getClass().getName(), e);
				}
			} else if (entity instanceof AbstractPersistentCollection) {
				AbstractPersistentCollection collection = (AbstractPersistentCollection) entity;
//				try {
//					if (persistentCollection.getSession() == null) {
////						persistentCollection.setCurrentSession((SessionImpl) s);
////						persistentCollection.forceInitialization();
//
//
//					}
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
				Object value = collection.getValue();
				try {
					if (value instanceof Collection && isInitialized(value)) {
						@SuppressWarnings("unchecked")
						T result = (T) value;
						return result;
					} else if (s instanceof SessionImpl) {
						SessionImpl session = (SessionImpl) s;
						CollectionPersister persister = ((SessionFactoryImpl) sessionFactory).getCollectionPersister(collection.getRole());
						collection.beforeInitialize(persister, -1);
						session.getPersistenceContext().addUninitializedDetachedCollection(persister, collection);
						if (collection.getSession() == null) {
							collection.setCurrentSession(session);
						}
						session.initializeCollection(collection, false);

//						CollectionPersister collectionPersister = ((SessionFactoryImpl) sessionFactory).getCollectionPersister(persistentCollection.getRole());
//						collectionPersister.initialize(persistentCollection.getKey(), (SessionImpl) s);
//						Object o = collectionPersister.getCollectionType().instantiate(20);
						value = collection.getValue();
						if (value instanceof Collection) {
							if (value instanceof PersistentBag || value instanceof PersistentList) {
								@SuppressWarnings("unchecked")
								T result = (T) new ArrayList<T>((Collection<T>) value);
								return result;
							} else if (value instanceof PersistentSet) {
								@SuppressWarnings("unchecked")
								T result = (T) new HashSet<T>((Collection<T>) value);
								return result;
							}
						} else {
							@SuppressWarnings("unchecked")
							T result = (T) value;
							return result;
						}

//						session.initializeCollection(persistentCollection, false);


//						CollectionEntry entry = ((SessionImpl) s).getPersistenceContext().getCollectionEntry(persistentCollection);
//						return entry == null ? : entry.;
					}
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Error initializing lazy collection: " + collection.getRole() + ", ID: " + collection.getKey(), e);
				}
			}

			if (canInitialize) {
				entity = initializeAndUnproxy(entity);
			} else {
				LOGGER.warning("Can not initialize " + entity.getClass().getName());
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error initializing entity", e);
		} finally {
			if (closeTransaction && transaction != null) {
				transaction.commit();
			}
			if (closeSession && (s != null && s.isOpen())) {
				s.close();
			}
		}

		return entity;
	}

	@Override
	public void doInitializeCaching(Query query, String cacheRegion) {
		if (query instanceof org.hibernate.Query) {
			org.hibernate.Query hQuery = (org.hibernate.Query) query;
			hQuery.setCacheable(true);
			if (cacheRegion != null) {
				hQuery.setCacheRegion(cacheRegion);
			}
		} else if (query instanceof HibernateQuery) {
			HibernateQuery hQuery = (HibernateQuery) query;
			org.hibernate.Query tmp = hQuery.getHibernateQuery();
			tmp.setCacheable(true);
			if (cacheRegion != null) {
				tmp.setCacheRegion(cacheRegion);
			}
		} else {
			LOGGER.warning("Query is not type of " + org.hibernate.Query.class.getName() + ", can not use caching");
		}
	}

	private <T> Map<String, List<T>> getCache(String name) {
		return IWCacheManager2.getInstance(IWMainApplication.getDefaultIWMainApplication()).getCache(name, 30000000, true, false, 86400);
	}

	@Override
	public <T> void setCache(String name, List<T> entities) {
		Map<String, List<T>> cache = getCache(name);
		cache.put(name, entities);
	}

	@Override
	public <T> List<T> getCachedEntities(String name) {
		Map<String, List<T>> cache = getCache(name);
		return cache.get(name);
	}

}