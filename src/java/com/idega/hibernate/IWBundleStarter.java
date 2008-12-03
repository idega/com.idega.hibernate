package com.idega.hibernate;

import com.idega.idegaweb.IWBundle;
import com.idega.idegaweb.IWBundleStartable;

public class IWBundleStarter implements IWBundleStartable {

	public void start(IWBundle arg0) {
		//hibernate is started from spring from the core bundle
		//tryHibernate();
	}

	/*private void tryHibernate() {
		try{
			new Test();
		}
		catch(Throwable t){
			t.printStackTrace();
		}
	}*/


	public void stop(IWBundle arg0) {
		// TODO Auto-generated method stub
	}
}
