package com.idega.hibernate;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Hibernate;
import org.hibernate.Transaction;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.transaction.annotation.Transactional;

import com.idega.util.DBUtil;

@Transactional(readOnly = true)
public class HibernateUtil extends DBUtil {

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

	public boolean isInitialized(Object object) {
		return Hibernate.isInitialized(object);
	}

	@Override
	public <T> T lazyLoad(T entity) {
		org.hibernate.Session s = null;
		Transaction transaction = null;
		try {
			s = SessionFactoryHelper.getInstance().getSessionFactory().openSession();
			transaction = s.beginTransaction();
			if (!s.isConnected()) {
				Logger.getLogger(getClass().getName()).warning("Session is not opened, can not lazy load entity!");
			}
			
			s.refresh(entity);
			entity = initializeAndUnproxy(entity);
		} catch (Exception e) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error initializing entity " + entity, e);
		} finally {
			transaction.commit();
			if (s.isOpen()) {
				s.close();
			}
		}

		return entity;
	}
	
}