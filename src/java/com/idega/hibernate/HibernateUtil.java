package com.idega.hibernate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.collection.internal.AbstractPersistentCollection;
import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.collection.internal.PersistentList;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.ejb.HibernateQuery;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
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

	private <T> T getRefreshed(Session session, T entity, boolean printError) {
		Transaction transaction = null;
		boolean closeTransaction = false;
		try {
			transaction = session.getTransaction();
			if (transaction == null || !transaction.isActive()) {
				closeTransaction = true;
				transaction = session.beginTransaction();
			}
			if (!session.isConnected()) {
				LOGGER.warning("Session is not opened, can not lazy load " + entity.getClass().getName());
			}

			session.refresh(entity);

			entity = initializeAndUnproxy(entity);
		} catch (Exception e) {
			if (printError) {
				LOGGER.log(Level.WARNING, "Error refreshing entity " + entity.getClass().getName(), e);
			}
			return null;
		} finally {
			if (closeTransaction && transaction != null) {
				transaction.commit();
			}
		}

		return entity;
	}

	@Override
	public <T> T lazyLoad(T entity) {
		if (isInitialized(entity)) {
			return entity;
		}

		return lazyLoad(null, entity, false);
	}

	private <T> T lazyLoad(Session s, T entity, boolean closeSession) {
		boolean closeTransaction = false;
		Transaction transaction = null;
		try {
			SessionFactory sessionFactory = SessionFactoryHelper.getInstance().getSessionFactory();
			if (s == null) {
				try {
					s = sessionFactory.getCurrentSession();
				} catch (Exception e) {}
			}

			if (s == null || !s.isOpen()) {
				closeSession = true;
				s = sessionFactory.openSession();
			}

			if (entity instanceof HibernateProxy) {
				T result = getRefreshed(s, entity, false);
				if (result == null && !(s instanceof SessionImpl)) {
					closeSession = true;
					s = sessionFactory.openSession();

					result = getRefreshed(s, entity, true);
				}
				return result;
			}

			if (entity instanceof AbstractPersistentCollection) {
				AbstractPersistentCollection collection = (AbstractPersistentCollection) entity;
				Object value = collection.getValue();
				try {
					if (value instanceof Collection && isInitialized(value)) {
						@SuppressWarnings("unchecked")
						T result = (T) value;
						return result;
					} else {
						SessionImplementor sessionImplementor = collection.getSession();
						SessionImpl session = sessionImplementor instanceof SessionImpl ? (SessionImpl) sessionImplementor : null;

						InvocationHandler invocationHandler = null;
						PersistenceContext persistenceContext = null;
						if (session == null && s instanceof SessionImpl) {
							session = (SessionImpl) s;
						} else if (session == null) {
							//	Session may be proxied
							String proxiedSessionClassName = null;
							if (java.lang.reflect.Proxy.isProxyClass(s.getClass())) {
								transaction = s.getTransaction();
								if (transaction == null || !transaction.isActive()) {
									closeTransaction = true;
									transaction = s.beginTransaction();
								}

								invocationHandler = Proxy.getInvocationHandler(s);
								Method getClass = Object.class.getMethod("getClass");
								Object className = null;
								try {
									className = invocationHandler.invoke(s, getClass, null);
								} catch (Throwable e) {
									LOGGER.log(Level.WARNING, "Error getting name of proxied session class, can not initialize entity " + entity.getClass().getName(), e);
								}
								if (className instanceof Class<?> && SessionImpl.class.getName().equals(((Class<?>) className).getName())) {
									proxiedSessionClassName = ((Class<?>) className).getName();
								} else {
									closeSession = true;
									s = sessionFactory.openSession();
								}
							}
							if (proxiedSessionClassName.equals(SessionImpl.class.getName()) && invocationHandler != null) {
								try {
									persistenceContext = (PersistenceContext) invocationHandler.invoke(s, SessionImpl.class.getMethod("getPersistenceContext"), null);
								} catch (Throwable e) {
									LOGGER.log(Level.WARNING, "Error getting persistence context, can not initialize " + collection.getRole() + ", ID: " + collection.getKey(), e);
								}
							}
						}

						if (session != null && persistenceContext == null) {
							persistenceContext = session.getPersistenceContext();
						}
						if (persistenceContext == null) {
							LOGGER.warning("Persistence context is unknown, can not initialize " + collection.getRole() + ", ID: " + collection.getKey());
							return null;
						}

						CollectionPersister persister = ((SessionFactoryImpl) sessionFactory).getCollectionPersister(collection.getRole());
						collection.beforeInitialize(persister, -1);
						if (persistenceContext.getCollection(new CollectionKey(persister, collection.getKey())) == null) {
							persistenceContext.addUninitializedDetachedCollection(persister, collection);
						}
						if (collection.getSession() == null && session != null) {
							collection.setCurrentSession(session);
						}
						if (session == null) {
							try {
								Method initializeCollection = SessionImpl.class.getMethod("initializeCollection", PersistentCollection.class, boolean.class);
								invocationHandler.invoke(
										s,
										initializeCollection,
										new Object[] {collection, false}
								);
							} catch (Throwable e) {
								LOGGER.log(Level.WARNING, "Error initializing collection: " + collection.getRole() + ", ID: " + collection.getKey() + " with session " + s.getClass().getName());
							}
						} else {
							session.initializeCollection(collection, false);
						}

						value = collection.getValue();
						boolean initialized = collection.wasInitialized();
						if (!initialized && session == null) {
							T result = lazyLoad(sessionFactory.openSession(), entity, true);
							return result;
						}

						if (value instanceof Collection) {
							if (value instanceof PersistentBag || value instanceof PersistentList) {
								@SuppressWarnings("unchecked")
								T result = initialized ? (T) new ArrayList<T>((Collection<T>) value) : (T) new ArrayList<T>(0);
								return result;
							} else if (value instanceof PersistentSet) {
								@SuppressWarnings("unchecked")
								T result = initialized ? (T) new HashSet<T>((Collection<T>) value) : (T) new HashSet<T>(0);
								return result;
							}
						} else {
							@SuppressWarnings("unchecked")
							T result = (T) value;
							return result;
						}
					}
				} catch (Exception e) {
					LOGGER.log(Level.WARNING, "Error initializing lazy collection: " + collection.getRole() + ", ID: " + collection.getKey(), e);
				}
			}
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error initializing entity", e);
		} finally {
			if (closeTransaction && transaction != null) {
				try {
					transaction.commit();
				} catch (Exception e) {}
			}
			if (closeSession && (s != null && s.isOpen())) {
				try {
					s.close();
				} catch (Exception e) {}
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