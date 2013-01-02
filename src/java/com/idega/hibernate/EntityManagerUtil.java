/**
 * $Id: EntityManagerUtil.java,v 1.1 2006/09/19 23:58:12 tryggvil Exp $
 * Created in 2006 by tryggvil
 *
 * Copyright (C) 2000-2006 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 */
package com.idega.hibernate;

import java.sql.Connection;
import java.util.Properties;
import java.util.logging.Logger;

import javax.persistence.EntityManagerFactory;

import org.hibernate.ejb.Ejb3Configuration;

import com.idega.core.persistence.EntityManagerProvider;
import com.idega.data.DatastoreInterface;
import com.idega.hibernate.demo.CarBean;
import com.idega.hibernate.demo.Driver;
import com.idega.util.database.ConnectionBroker;
import com.idega.util.database.PoolManager;

/**
 * <p>
 * Class for initializing the Hibernate EJB3 EntityManager within idegaWeb
 * ePlatform
 * </p>
 * Last modified: $Date: 2006/09/19 23:58:12 $ by $Author: tryggvil $
 * 
 * @author <a href="mailto:tryggvil@idega.com">tryggvil</a>
 * @version $Revision: 1.1 $
 */
public class EntityManagerUtil implements EntityManagerProvider {

	private static EntityManagerFactory sessionFactory = null;

	public static void configure() {
		try {
			Logger loggerRoot = Logger.getLogger(HibernateUtil.class.getName());
			Logger loggerConnect = Logger.getLogger("Connect");

			loggerRoot.fine("In EntityManagerUtil try-clause");
			loggerRoot.warning("In EntityManagerUtil try-clause");
			loggerConnect
					.fine("In EntityManagerUtil try-clause via loggerConnect DEBUG*****");

			// Create the SessionFactory from hibernate.cfg.xml
			Properties properties = getProperties();
			Ejb3Configuration configuration = new Ejb3Configuration();
			configuration.setProperties(properties);
			configuration.addAnnotatedClass(CarBean.class);
			configuration.addAnnotatedClass(Driver.class);
			sessionFactory = configuration.buildEntityManagerFactory();
		} catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			System.err
					.println("EntityManagerUtil: Initial SessionFactory creation failed."
							+ ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	private static Properties getProperties() {
		Properties prop = new Properties();
		if (ConnectionBroker.isUsingIdegaPool()) {
			PoolManager pmgr = PoolManager.getInstance();
			prop.put("hibernate.connection.driver_class",
					pmgr.getDriverClassForPool());
			prop.put("hibernate.connection.url", pmgr.getURLForPool());
			prop.put("hibernate.connection.username", pmgr.getUserNameForPool());
			prop.put("hibernate.connection.password", pmgr.getPasswordForPool());
			detectDialect(prop);
		} else if (ConnectionBroker.isUsingJNDIDatasource()) {
			String prefix = "java:comp/env/";
			prop.put("hibernate.connection.datasource", prefix
					+ ConnectionBroker.getDefaultJNDIUrl());
			detectDialect(prop);
		}

		prop.put("hibernate.cache.provider_class",
				IWCacheProvider.class.getName());

		return prop;
	}

	private static void detectDialect(Properties prop) {

		Connection conn = null;
		try {
			conn = ConnectionBroker.getConnection();
			String dsType = DatastoreInterface.getDataStoreType(conn);
			String dialectClass = null;
			if (dsType.equals(DatastoreInterface.DBTYPE_DB2)) {
				dialectClass = org.hibernate.dialect.DB2Dialect.class.getName();
			} else if (dsType.equals(DatastoreInterface.DBTYPE_DERBY)) {
				dialectClass = org.hibernate.dialect.DerbyDialect.class	.getName();
			}
			else if (dsType.equals(DatastoreInterface.DBTYPE_HSQL)) {
				dialectClass = org.hibernate.dialect.HSQLDialect.class.getName();
			} else if (dsType.equals(DatastoreInterface.DBTYPE_INFORMIX)) {
				dialectClass = org.hibernate.dialect.InformixDialect.class	.getName();
			} else if (dsType.equals(DatastoreInterface.DBTYPE_INTERBASE)) {
				dialectClass = org.hibernate.dialect.FirebirdDialect.class
						.getName();
			} else if (dsType.equals(DatastoreInterface.DBTYPE_MCKOI)) {
				dialectClass = org.hibernate.dialect.MckoiDialect.class
						.getName();
			} else if (dsType.equals(DatastoreInterface.DBTYPE_MSSQLSERVER)) {
				dialectClass = org.hibernate.dialect.SQLServerDialect.class
						.getName();
			} else if (dsType.equals(DatastoreInterface.DBTYPE_MYSQL)) {
				dialectClass = org.hibernate.dialect.MySQLDialect.class
						.getName();
			} else if (dsType.equals(DatastoreInterface.DBTYPE_ORACLE)) {
				dialectClass = org.hibernate.dialect.Oracle8iDialect.class
						.getName();
			} else if (dsType.equals(DatastoreInterface.DBTYPE_ORACLE)) {
				dialectClass = org.hibernate.dialect.Oracle8iDialect.class
						.getName();
			} else if (dsType.equals(DatastoreInterface.DBTYPE_SAPDB)) {
				dialectClass = org.hibernate.dialect.SAPDBDialect.class
						.getName();
			}

			prop.put("hibernate.dialect", dialectClass);
		} finally {
			ConnectionBroker.freeConnection(conn);
		}

	}

	public EntityManagerFactory getEntityManagerFactory() {
		return getEntityManagerFactoryStatic();
	}

	public static EntityManagerFactory getEntityManagerFactoryStatic() {
		if (sessionFactory == null) {
			configure();
		}
		return sessionFactory;
	}
}
