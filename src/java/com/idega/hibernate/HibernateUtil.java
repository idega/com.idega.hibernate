package com.idega.hibernate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.Query;

import org.hibernate.Hibernate;
import org.hibernate.Transaction;
import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.ejb.HibernateQuery;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.cache.IWCacheManager2;
import com.idega.idegaweb.IWMainApplication;
import com.idega.util.ArrayUtil;
import com.idega.util.DBUtil;

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

		org.hibernate.Session s = null;
		Transaction transaction = null;
		try {
			s = SessionFactoryHelper.getInstance().getSessionFactory().openSession();
			transaction = s.beginTransaction();
			if (!s.isConnected()) {
				LOGGER.warning("Session is not opened, can not lazy load entity!");
			}

			if (entity instanceof HibernateProxy) {
				try {
					s.refresh(entity);
				} catch (ClassCastException e) {
					LOGGER.log(Level.WARNING, "Error refreshing entity", e);
				}
			} else if (entity instanceof PersistentBag) {
				PersistentBag persistentBag = (PersistentBag) entity;
				if (persistentBag.getSession() == null) {
					Object[] entities = persistentBag.toArray();
					if (!ArrayUtil.isEmpty(entities)) {
						@SuppressWarnings("unchecked")
						T results = (T) new ArrayList<T>((Collection<? extends T>) Arrays.asList(entities));
						return results;
					}
				} else {
					LOGGER.info("Persistent bag has session!");
				}
			}
			entity = initializeAndUnproxy(entity);
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error initializing entity", e);
		} finally {
			transaction.commit();
			if (s.isOpen()) {
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