/**
 * $Id: Test.java,v 1.2 2006/11/28 18:44:10 laddi Exp $
 * Created in 2006 by tryggvil
 *
 * Copyright (C) 2000-2006 Idega Software hf. All Rights Reserved.
 *
 * This software is the proprietary information of Idega hf.
 * Use is subject to license terms.
 */
package com.idega.hibernate.demo;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import com.idega.hibernate.EntityManagerUtil;


/**
 * <p>
 * Class for testing if Hibernate works in eplatform
 * </p>
 *  Last modified: $Date: 2006/11/28 18:44:10 $ by $Author: laddi $
 * 
 * @author <a href="mailto:tryggvil@idega.com">tryggvil</a>
 * @version $Revision: 1.2 $
 */
public class Test {
	
	
	public Test(){
		tryHibernate();
	}
	
	public void tryHibernate() {
		EntityManagerFactory factory = EntityManagerUtil.getInstance().getEntityManagerFactory();
		EntityManager session = factory.createEntityManager();
		
		CarBean bigSUV = new CarBean();
		bigSUV.setManufacturer("Ford");
		bigSUV.setModel("Expedition");
		bigSUV.setYear(2005);
		Driver jill = getDriverByName("Jill", session);
		addCarToDriver(jill, bigSUV, session);
		
		session.close();
	}

	private void addCarToDriver(Driver theOwner, CarBean theCar, EntityManager session) {
		theOwner.getCarsOwned().add(theCar);
		session.persist(theCar);
		session.persist(theOwner);
		
	}

	public Driver getDriverByName(String name, EntityManager session) {
		Query q = session.createQuery("from Driver d where d.name = :name");
		q.setParameter("name", name);
		Driver newOwner = (Driver) q.getResultList().get(0);
		return newOwner;
	}

}
