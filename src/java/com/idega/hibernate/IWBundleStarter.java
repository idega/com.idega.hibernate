package com.idega.hibernate;

import org.springframework.beans.factory.annotation.Autowired;

import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWBundleStartable;
import com.idega.util.expression.ELUtil;
import com.idega.util.timer.IWTaskScheduler;

public class IWBundleStarter implements IWBundleStartable {

	@Autowired
	private IWTaskScheduler taskScheduler;

	private IWTaskScheduler getTaskScheduler() {
		if (taskScheduler == null) {
			ELUtil.getInstance().autowire(this);
		}
		return taskScheduler;
	}

	@Override
	public void start(IWBundle bundle) {
		IWTaskScheduler taskScheduler = getTaskScheduler();
		try {
			taskScheduler.schedule(0, -1, -1, ((HibernateUtil) HibernateUtil.getInstance()).getTaskToFinalizeSessions());
		} catch (Exception e) {}

	}

	@Override
	public void stop(IWBundle bundle) {
	}

}