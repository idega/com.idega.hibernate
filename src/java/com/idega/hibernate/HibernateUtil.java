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


import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Configuration;

import com.idega.data.DatastoreInterface;
import com.idega.util.database.ConnectionBroker;
import com.idega.util.database.PoolManager;

/**
 * <p>
 * Class to initialize hibernate in eplatform
 * </p>
 *  Last modified: $Date: 2007/09/17 13:32:08 $ by $Author: civilis $
 * 
 * @author <a href="mailto:tryggvil@idega.com">tryggvil</a>
 * @version $Revision: 1.3 $
 */
public class HibernateUtil { 

    public static SessionFactory configure() {
        try {
	     Logger loggerRoot = Logger.getLogger(HibernateUtil.class.getName());
	     Logger loggerConnect = Logger.getLogger("Connect");
    		
	     loggerRoot.fine("In HibernateUtil try-clause");
	     loggerRoot.warning("In HibernateUtil try-clause");
	     loggerConnect.fine("In HibernateUtil try-clause via loggerConnect DEBUG*****");

            // Create the SessionFactory from hibernate.cfg.xml
	     	//SettingsFactory settingsfactory = new InjectionSettingsFactory();
	     	Properties properties = getProperties();
			//settingsfactory.buildSettings(properties);
			Configuration configuration = new Configuration();
			
			configuration.setProperties(properties);
			//Configuration conf2 = configuration.configure();
            return configuration.buildSessionFactory();
        } catch (Throwable ex) {
            // Make sure you log the exception, as it might be swallowed
            System.err.println("Initial SessionFactory creation failed." + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static Properties getProperties() {
    	Properties prop = new Properties();
    	if(ConnectionBroker.isUsingIdegaPool()){
    		PoolManager pmgr = PoolManager.getInstance();
    		prop.put("hibernate.connection.driver_class", pmgr.getDriverClassForPool());
    		prop.put("hibernate.connection.url", pmgr.getURLForPool());
    		prop.put("hibernate.connection.username", pmgr.getUserNameForPool());
    		prop.put("hibernate.connection.password", pmgr.getPasswordForPool());
    		detectDialect(prop);
    	}
    	else if(ConnectionBroker.isUsingJNDIDatasource()){
    		String prefix = "java:comp/env/";
    		prop.put("hibernate.connection.datasource", prefix+ConnectionBroker.getDefaultJNDIUrl());
    		detectDialect(prop);
    	}
    	
    	prop.put("hibernate.cache.provider_class", IWCacheProvider.class.getName());
    	//prop.put("hibernate.cache.provider_class", EhCacheProvider.class.getName());
    	
    	return prop;
	}
    
    private static void detectDialect(Properties prop) {
    	
    	detectDialect(prop, null);
    }

	@SuppressWarnings("deprecation")
	private static void detectDialect(Properties prop, String dataSourceName) {
		
		Connection conn = null;
		try{	
			conn = dataSourceName == null ? ConnectionBroker.getConnection() : ConnectionBroker.getConnection(dataSourceName);
			
			String dsType = DatastoreInterface.getDataStoreType(conn);
			String dialectClass = null;
			if(dsType.equals(DatastoreInterface.DBTYPE_DB2)){
				dialectClass = org.hibernate.dialect.DB2Dialect.class.getName();
			}
			else if(dsType.equals(DatastoreInterface.DBTYPE_DERBY)){
				dialectClass = org.hibernate.dialect.DerbyDialect.class.getName();
			}
			//else if(dsType.equals(DatastoreInterface.DBTYPE_H2)){
			//	dialectClass = org.hibernate.dialect.H2Dialect.class.getName();
			//}
			else if(dsType.equals(DatastoreInterface.DBTYPE_HSQL)){
				dialectClass = org.hibernate.dialect.HSQLDialect.class.getName();
			}
			else if(dsType.equals(DatastoreInterface.DBTYPE_INFORMIX)){
				dialectClass = org.hibernate.dialect.InformixDialect.class.getName();
			}
			else if(dsType.equals(DatastoreInterface.DBTYPE_INTERBASE)){
				dialectClass = org.hibernate.dialect.FirebirdDialect.class.getName();
			}
			else if(dsType.equals(DatastoreInterface.DBTYPE_MCKOI)){
				dialectClass = org.hibernate.dialect.MckoiDialect.class.getName();
			}
			else if(dsType.equals(DatastoreInterface.DBTYPE_MSSQLSERVER)){
				dialectClass = org.hibernate.dialect.SQLServerDialect.class.getName();
			}
			else if(dsType.equals(DatastoreInterface.DBTYPE_MYSQL)){
				dialectClass = org.hibernate.dialect.MySQLDialect.class.getName();
			}
//			else if(dsType.equals(DatastoreInterface.DBTYPE_ORACLE)){ 						-- dublicated
//				dialectClass = org.hibernate.dialect.OracleDialect.class.getName();
//			}
			else if(dsType.equals(DatastoreInterface.DBTYPE_ORACLE)){
				dialectClass = org.hibernate.dialect.OracleDialect.class.getName();
			}
			else if(dsType.equals(DatastoreInterface.DBTYPE_SAPDB)){
				dialectClass = org.hibernate.dialect.SAPDBDialect.class.getName();
			}
			
			prop.put("hibernate.dialect", dialectClass);
		}
		finally{
			ConnectionBroker.freeConnection(conn);
		}
	
	}

	public static SessionFactory getSessionFactory() {
		
		return getSessionFactory(null);
    }
	
	public static synchronized SessionFactory getSessionFactory(String cfgPath) {
		
		String confPath = cfgPath == null || cfgPath.equals("") ? "default" : cfgPath;

		Map<String, SessionFactory> sessionFactories = getInitializedSessionFactories();
		
		if(sessionFactories.containsKey(confPath))
			return sessionFactories.get(confPath);
		
		SessionFactory sf = cfgPath == null || cfgPath.equals("") ? configure() : configure(cfgPath);
		
		sessionFactories.put(confPath, sf);
		return sf;
    }
	
	private static Map<String, SessionFactory> initializedSessionFactories;
	
	private static Map<String, SessionFactory> getInitializedSessionFactories() {
		
		if(initializedSessionFactories == null)
			initializedSessionFactories = new HashMap<String, SessionFactory>();
		
		return initializedSessionFactories;
	}
	
	private static SessionFactory configure(String cfgPath){
        try {
            
			Configuration configuration = new AnnotationConfiguration();

			if(cfgPath == null)
				configuration.configure();
			else
				configuration.configure(cfgPath);
			
            return configuration.buildSessionFactory();
            
        } catch (Throwable ex) {
        	
        	Logger.getLogger(HibernateUtil.class.getName()).log(Level.SEVERE, "Initial SessionFactory creation failed for cfg path: "+cfgPath, ex);
            throw new ExceptionInInitializerError(ex);
        }
    }
}