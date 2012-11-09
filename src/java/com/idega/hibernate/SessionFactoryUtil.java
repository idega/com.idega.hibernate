/**
 * $Id: HibernateUtil.java,v 1.3 2007/09/17 13:32:08 civilis Exp $
 * Created in 2006 by tryggvil
 *
 * Copyright (C) 2000-2006 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 */
package com.idega.hibernate;


import java.lang.reflect.Method;
import java.util.logging.Level;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.springframework.orm.hibernate3.HibernateTransactionManager;

/**
 * <p>
 * Class to initialize hibernate in eplatform
 * </p>
 * Last modified: $Date: 2007/09/17 13:32:08 $ by $Author: civilis $
 * 
 *  Last modified: $Date: 2007/09/17 13:32:08 $ by $Author: civilis $
 *
 * @author <a href="mailto:tryggvil@idega.com">tryggvil</a>
 * @version $Revision: 1.3 $
 */
public class SessionFactoryUtil extends HibernateUtil {

	protected static HibernateTransactionManager hibernateTransactionManager;
	
	/**
	 * <p>Locates "idegaHibernateTransactionManager" spring bean, if not 
	 * initiated.</p>
	 * @return {@link HibernateTransactionManager} or <code>null</code> on failure.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public HibernateTransactionManager getHibernateTransactionManager() {
		return getHibernateTransactionManager("idegaHibernateTransactionManager");
	}
	
	/**
	 * <p>Locates spring bean for given transaction manager name, if not 
	 * initiated.</p>
	 * @param transactionManagerBeanName - name in application context of 
	 * hibernate transaction manager.
	 * @return {@link HibernateTransactionManager} or <code>null</code> on failure.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	protected HibernateTransactionManager getHibernateTransactionManager(
			String transactionManagerBeanName) {
		if (transactionManagerBeanName == null || transactionManagerBeanName.isEmpty()) {
			return null;
		}
		
		if (hibernateTransactionManager != null) {
			return hibernateTransactionManager;
		}
		
		Object bean = getApplicationContext().getBean(transactionManagerBeanName);
		if (bean instanceof HibernateTransactionManager) {
			hibernateTransactionManager = (HibernateTransactionManager) bean;
			return hibernateTransactionManager;
		}
		
		LOGGER.log(Level.WARNING, HibernateTransactionManager.class + " bean" +
				" not found. Check your configuration, or maybe spring not " +
				"initated yet");
		return null;
	}

	private static SessionFactoryUtil util = null;

	protected SessionFactoryUtil() {
		util = this;
	}

	public static final SessionFactoryUtil getInstance() {
		if (util == null)
			util = new SessionFactoryUtil();
		return util;
	}

	/**
	 * <p>Gets current session or opens new session.</p>
	 * @return {@link Session} or <code>null</code> on failure.
	 * @throws HibernateException - means serious problem.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public static Session getSession() throws HibernateException {
		SessionFactory sessionFactory = getSessionFactory();
		if (sessionFactory == null) {
			LOGGER.warning("No " + SessionFactory.class + " found.");
			return null;
		}
		
		Session session = null;
		try {
			sessionFactory.getCurrentSession();
		} catch (HibernateException e) {
			LOGGER.log(Level.INFO, "No current " + Session.class + 
					" configured! We'll open new one.");
		}
		
		if (session == null) {
			session = sessionFactory.openSession();
		}
				
		return session;
	}
		
	/**
	 * 
	 * @param getter the method which returns field that should be loaded
	 * @param entity entity that contains the field
	 * @param getterParameters getter's parameters
	 * @return returns the new instance of entity that has it's field initialized.
	 */
	public Object loadLazyField(Method getter,Object entity,Object... getterParameters) {
		Session session = null;
		Object updated = null;
		try {
			session = getSession();
			Transaction transaction = session.beginTransaction();
			updated = session.merge(entity);
			Object value = getter.invoke(updated, getterParameters);
			Hibernate.initialize(value);
			transaction.commit();
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "Error loading lazy value", e);
		} finally {
			if ((session != null) && session.isOpen()){
				session.close();
			}
		}
		return updated;
	}

	/**
	 * <p>If does not exist, creates it.</p>
	 * @return {@link SessionFactory} or <code>null</code> on failure.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public static SessionFactory getSessionFactory() {
		SessionFactoryUtil hu = getInstance();
		if (hu == null) {
			LOGGER.log(Level.WARNING, "Unable to get instance of: " + 
					SessionFactoryUtil.class);
			return null;
		}
		
		HibernateTransactionManager htm = hu.getHibernateTransactionManager();
		if (htm == null) {
			LOGGER.log(Level.WARNING, "Unable to get transaction manager");
			return null;
		}
		
		return htm.getSessionFactory();
	}
}