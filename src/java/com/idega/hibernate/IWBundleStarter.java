package com.idega.hibernate;

import com.idega.hibernate.demo.Test;
import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWBundleStartable;

public class IWBundleStarter implements IWBundleStartable {

	public void start(IWBundle arg0) {
		// TODO Auto-generated method stub
		tryHibernate();
	}

	private void tryHibernate() {
		try{
			Test test = new Test();
		}
		catch(Throwable t){
			t.printStackTrace();
		}
	}


	public void stop(IWBundle arg0) {
		// TODO Auto-generated method stub
	}
}
