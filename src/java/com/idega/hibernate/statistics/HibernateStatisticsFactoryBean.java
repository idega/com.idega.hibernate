package com.idega.hibernate.statistics;

import javax.persistence.EntityManagerFactory;

import org.hibernate.SessionFactory;
import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.stat.Statistics;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.idega.util.expression.ELUtil;

@Component
public class HibernateStatisticsFactoryBean implements FactoryBean<Statistics> {

	@Autowired(required = false)
	private EntityManagerFactory entityManagerFactory;

	private EntityManagerFactory getEntityManagerFactory() {
		if (entityManagerFactory == null) {
			ELUtil.getInstance().autowire(this);
		}
		return entityManagerFactory;
	}

	@Override
	public Statistics getObject() throws Exception {
		SessionFactory sessionFactory = ((HibernateEntityManagerFactory) getEntityManagerFactory()).getSessionFactory();
		return sessionFactory.getStatistics();
	}

	@Override
	public Class<?> getObjectType() {
		return Statistics.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}