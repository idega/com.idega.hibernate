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

import java.util.logging.Level;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.orm.jpa.JpaTransactionManager;


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
public class EntityManagerUtil extends HibernateUtil {
	
	private static final EntityManagerUtil emu = new EntityManagerUtil();
	
	public static EntityManagerUtil getInstance() {
		return emu;
	}
	
	protected EntityManagerUtil() {}
	
	private JpaTransactionManager jpaTransactionManager = null;
	
	/**
	 * <p>Gets JPA transaction manager.</p>
	 * @return {@link JpaTransactionManager} or <code>null</code> on failure.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public JpaTransactionManager getJpaTransactionManager() {
		return getJpaTransactionManager(TRANSACTION_MANAGER_NAME);
	}
	
	/**
	 * <p>Gets JPA transaction manager.</p>
	 * @return {@link JpaTransactionManager} or <code>null</code> on failure.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	protected JpaTransactionManager getJpaTransactionManager(String name) {	
		// FIXME check if it is correct one;
		if (jpaTransactionManager != null) {
			return jpaTransactionManager;
		}
		
		Object bean = getApplicationContext().getBean(name);
		if (bean instanceof JpaTransactionManager) {
			jpaTransactionManager = (JpaTransactionManager) bean;
			return jpaTransactionManager;
		}
		
		LOGGER.log(Level.WARNING, JpaTransactionManager.class + " bean" +
				" not found. Check your configuration, or maybe spring not " +
				"initated yet");
		return null;
	}
	
	/**
	 * <p>Gets {@link EntityManagerFactory} from {@link JpaTransactionManager}.
	 * </p>
	 * @return {@link EntityManagerFactory} or <code>null</code> on failure.
	 * @author <a href="mailto:martynas@idega.com">Martynas Stakė</a>
	 */
	public EntityManagerFactory getEntityManagerFactory() {
		JpaTransactionManager jtm = getJpaTransactionManager();
		if (jtm == null) {
			LOGGER.log(Level.WARNING, "No " + JpaTransactionManager.class + 
					" found. Please check your dependencies and configuration.");
			return null;
		}

		return jtm.getEntityManagerFactory();
	}
	
	public EntityManager getEntityManager() {
		return getEntityManagerFactory().createEntityManager();
	}
}
