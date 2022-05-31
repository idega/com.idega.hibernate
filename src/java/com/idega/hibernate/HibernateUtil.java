package com.idega.hibernate;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.internal.SessionImpl;
import org.hibernate.jpa.HibernateQuery;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.proxy.HibernateProxy;
import org.hibernate.stat.Statistics;
import org.springframework.transaction.annotation.Transactional;

import com.idega.core.cache.IWCacheManager2;
import com.idega.idegaweb.IWMainApplication;
import com.idega.util.CoreUtil;
import com.idega.util.DBUtil;
import com.idega.util.ListUtil;
import com.idega.util.datastructures.map.MapUtil;

import net.sf.ehcache.util.concurrent.ConcurrentHashMap;

@Transactional(readOnly = true)
public class HibernateUtil extends DBUtil {

	private static final Logger LOGGER = Logger.getLogger(HibernateUtil.class.getName());

	private Map<Long, List<Session>> allSessions = new ConcurrentHashMap<>();

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
	public <T> void finalizeTransaction(T transaction) {
		try {
			if (!(transaction instanceof Transaction)) {
				LOGGER.warning("Can not finalize transaction: " + (transaction == null ? "not provided" : transaction.getClass().getName()));
				return;
			}

			Transaction hTransaction = (Transaction) transaction;
			if (hTransaction.wasCommitted()) {
				return;
			}

			hTransaction.commit();
		} catch (Exception e) {}
	}

	@Override
	public <S> void finalizeSession(S session) {
		finalizeSession(null, session);
	}

	private <S> void finalizeSession(Long timestamp, S session) {
		try {
			if (!(session instanceof Session)) {
				LOGGER.warning("Can not finalize session: " + (session == null ? "not provided" : session.getClass().getName()));
				return;
			}

			if (timestamp == null) {
				Session hSession = (Session) session;
				if (IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("hibernate.close_transaction_from_session", true)) {
					finalizeTransaction(hSession.getTransaction());
				}

				if (!hSession.isOpen()) {
					return;
				}

				hSession.close();
			} else {
				List<Session> sessions = allSessions.get(timestamp);
				if (sessions == null) {
					sessions = new ArrayList<>();
					allSessions.put(timestamp, sessions);
				}
				sessions.add((Session) session);
			}
		} catch (Exception e) {}
	}

	public Runnable getTaskToFinalizeSessions() {
		return new Runnable() {

			@Override
			public void run() {
				if (MapUtil.isEmpty(allSessions)) {
					return;
				}

				long now = System.currentTimeMillis();
				Set<Long> keys = new HashSet<>(allSessions.keySet());
				int minSessionAge = IWMainApplication.getDefaultIWMainApplication().getSettings().getInt("hibernate.session_min_age", 60 * 1000);
				for (Long key: keys) {
					long age = now - key;
					if (age < minSessionAge) {
						continue;
					}

					List<Session> sessions = allSessions.remove(key);
					if (ListUtil.isEmpty(sessions)) {
						continue;
					}

					for (Session session: sessions) {
						finalizeSession(null, session);
					}
				}
			}

		};
	}

	private <T> T getRefreshed(EventSource session, T entity, boolean printError, boolean closeSession) {
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
			if (closeTransaction) {
				finalizeTransaction(transaction);
			}
			if (closeSession) {
				finalizeSession(System.currentTimeMillis(), session);
			}
		}

		return entity;
	}

	@Override
	public <T> T lazyLoad(T entity) {
		try {
			if (isInitialized(entity)) {
				return entity;
			}

			boolean alwaysCloseSession = IWMainApplication.getDefaultIWMainApplication().getSettings().getBoolean("hibernate.close_session_after_lazy_load", true);
			if (entity instanceof HibernateProxy) {
				return lazyLoadProxy(null, entity, alwaysCloseSession);
			} else if (entity instanceof PersistentCollection) {
				return lazyLoadCollection(null, entity, alwaysCloseSession);
			}

			LOGGER.warning("Do not know how to lazy load entity " + entity.getClass().getName());
		} catch (Exception e) {
			String message = "Error while lazily loading entity " + entity.getClass().getName();
			CoreUtil.sendExceptionNotification(message, e);
			LOGGER.log(Level.WARNING, message, e);
		}
		return null;
	}

	private <T> T lazyLoadProxy(Session s, T proxy, boolean closeSession) {
		EventSource session = null;
		//	Checking current session
		if (s instanceof EventSource) {
			session = (EventSource) s;
		} else if (s == null) {
			SessionImplementor sessionImplementor = ((HibernateProxy) proxy).getHibernateLazyInitializer().getSession();
			if (sessionImplementor instanceof EventSource) {
				session = (SessionImpl) sessionImplementor;
			} else if (sessionImplementor != null) {
				LOGGER.warning(sessionImplementor.getClass().getName() + " is not EventSource!");
			}
		}

		if (session == null) {
			try {
				SessionWithFlag sessionWithFlag = getSession();
				s = sessionWithFlag == null ? null : sessionWithFlag.session;
				closeSession = sessionWithFlag == null ? false : sessionWithFlag.openedNewSession;
				if (s instanceof SessionImpl) {
					session = (SessionImpl) s;
				} else if (s instanceof EventSource) {
					session = (EventSource) s;
				} else if (s != null && Proxy.isProxyClass(s.getClass())) {
					session = (EventSource) s;
				}
			} catch (Exception e) {
				LOGGER.log(Level.WARNING, "Error getting current session " + s, e);
			}
		}
		boolean printError = false;
		if (session == null) {
			printError = true;
			closeSession = true;
			SessionFactory sessionFactory = SessionFactoryHelper.getInstance().getSessionFactory();
			s = sessionFactory.openSession();
			if (s instanceof SessionImpl) {
				session = (SessionImpl) s;
			} else if (s instanceof EventSource) {
				session = (EventSource) s;
			}
		}

		T result = getRefreshed(session, proxy, printError, closeSession);
		return result;
	}

	private <T> T lazyLoadCollection(Session s, T entity, boolean closeSession) {
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
						EventSource session = sessionImplementor instanceof EventSource ? (EventSource) sessionImplementor : null;

						InvocationHandler invocationHandler = null;
						PersistenceContext persistenceContext = null;
						if (session == null && s instanceof SessionImpl) {
							session = (SessionImpl) s;
						} else if (session == null) {
							//	Session may be proxied
							String proxiedSessionClassName = null;
							if (Proxy.isProxyClass(s.getClass())) {
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
							} catch (Throwable e) {}
						} else {
							session.initializeCollection(collection, false);
						}

						value = collection.getValue();
						boolean initialized = collection.wasInitialized();
						if (!initialized && session == null) {
							T result = lazyLoadCollection(sessionFactory.openSession(), entity, true);
							return result;
						}

						if (value instanceof Collection) {
							if (value instanceof PersistentBag || value instanceof PersistentList) {
								@SuppressWarnings("unchecked")
								T result = initialized ? (T) new ArrayList<>((Collection<T>) value) : (T) new ArrayList<T>(0);
								return result;
							} else if (value instanceof PersistentSet) {
								@SuppressWarnings("unchecked")
								T result = initialized ? (T) new HashSet<>((Collection<T>) value) : (T) new HashSet<T>(0);
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
			if (closeTransaction) {
				finalizeTransaction(transaction);
			}
			if (closeSession) {
				finalizeSession(System.currentTimeMillis(), s);
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

	@SuppressWarnings("unchecked")
	@Override
	public Session getCurrentSession() {
		SessionWithFlag sessionWithFlag = getSession();
		return sessionWithFlag == null ? null : sessionWithFlag.session;
	}

	@Override
	public String getStatistics() {
		SessionWithFlag sessionWithFlag = getSession();
		if (sessionWithFlag == null || sessionWithFlag.session == null) {
			return "Current session is not available";
		}

		try {
			SessionFactory sessionFactory = sessionWithFlag.session.getSessionFactory();
			Statistics statistics = sessionFactory.getStatistics();
			return statistics.toString();
		} finally {
			if (sessionWithFlag.openedNewSession) {
				finalizeSession(System.currentTimeMillis(), sessionWithFlag.session);
			}
		}
	}

	private class SessionWithFlag {

		private Session session = null;
		private boolean openedNewSession = false;

		private SessionWithFlag(Session session, boolean openedNewSession) {
			this.session = session;
			this.openedNewSession = openedNewSession;
		}

	}

	private SessionWithFlag getSession() {
		SessionFactory sessionFactory = SessionFactoryHelper.getInstance().getSessionFactory();
		Session session = sessionFactory.getCurrentSession();
		boolean openedNewSession = false;
		if (session == null || !session.isOpen()) {
			session = sessionFactory.openSession();
			openedNewSession = true;
		}
		return new SessionWithFlag(session, openedNewSession);
	}

	@Override
	public String getQueryInfo(Query q) {
		if (q == null) {
			return null;
		}

		if (q instanceof HibernateQuery) {
			return ((HibernateQuery) q).getHibernateQuery().toString();
		}

		LOGGER.warning("Do not know how to get info from " + q.getClass().getName());
		return q.toString();
	}

}