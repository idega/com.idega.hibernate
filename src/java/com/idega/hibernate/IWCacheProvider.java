package com.idega.hibernate;

import java.net.URL;
import java.util.Properties;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.hibernate.EhCacheProvider;
import net.sf.ehcache.util.ClassLoaderUtil;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.cache.Cache;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.CacheProvider;

import com.idega.core.cache.IWCacheManager2;
import com.idega.idegaweb.IWMainApplication;

/**
 * <p>
 * Special cache provider to ovverride the default Hibernate one
 * </p>
 *  Last modified: $Date: 2006/09/19 23:58:12 $ by $Author: tryggvil $
 * 
 * @author <a href="mailto:tryggvil@idega.com">tryggvil</a>
 * @version $Revision: 1.1 $
 */
@SuppressWarnings("deprecation")
public class IWCacheProvider implements CacheProvider {

	public static final String NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME = "net.sf.ehcache.configurationResourceName";
	private static final Log LOG = LogFactory.getLog(EhCacheProvider.class);
	private CacheManager manager;

	public Cache buildCache(String name, Properties properties) throws CacheException {
		try {
			net.sf.ehcache.Cache cache = this.manager.getCache(name);
			if (cache == null) {
				LOG.warn("Could not find a specific ehcache configuration for cache named [" + name
						+ "]; using defaults.");
				this.manager.addCache(name);
				cache = this.manager.getCache(name);
				LOG.debug("started EHCache region: " + name);
			}
			return new net.sf.ehcache.hibernate.EhCache(cache);
		}
		catch (net.sf.ehcache.CacheException e) {
			throw new CacheException(e);
		}
	}

	public boolean isMinimalPutsEnabledByDefault() {
		// TODO Auto-generated method stub
		return false;
	}

	public long nextTimestamp() {
		// TODO Auto-generated method stub
		return 0;
	}

	public void start(Properties properties) throws CacheException {
		if (this.manager != null) {
			LOG.warn("Attempt to restart an already started EhCacheProvider. Use sessionFactory.close() "
					+ " between repeated calls to buildSessionFactory. Using previously created EhCacheProvider."
					+ " If this behaviour is required, consider using SingletonEhCacheProvider.");
			return;
		}
		try {
			String configurationResourceName = null;
			if (properties != null) {
				configurationResourceName = (String) properties.get(NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME);
			}
			if (configurationResourceName == null || configurationResourceName.length() == 0) {
				this.manager = IWCacheManager2.getInstance(IWMainApplication.getDefaultIWMainApplication()).getInternalCacheManager();
			}
			else {
				if (!configurationResourceName.startsWith("/")) {
					configurationResourceName = "/" + configurationResourceName;
					if (LOG.isDebugEnabled()) {
						LOG.debug("prepending / to " + configurationResourceName + ". It should be placed in the root"
								+ "of the classpath rather than in a package.");
					}
				}
				URL url = loadResource(configurationResourceName);
				this.manager = new CacheManager(url);
			}
		}
		catch (net.sf.ehcache.CacheException e) {
			if (e.getMessage().startsWith(
					"Cannot parseConfiguration CacheManager. Attempt to create a new instance of "
							+ "CacheManager using the diskStorePath")) {
				throw new CacheException(
						"Attempt to restart an already started EhCacheProvider. Use sessionFactory.close() "
								+ " between repeated calls to buildSessionFactory. Consider using SingletonEhCacheProvider. Error from "
								+ " ehcache was: " + e.getMessage());
			}
			throw e;
		}
	}

	private URL loadResource(String configurationResourceName) {
		ClassLoader standardClassloader = ClassLoaderUtil.getStandardClassLoader();
		URL url = null;
		if (standardClassloader != null) {
			url = standardClassloader.getResource(configurationResourceName);
		}
		if (url == null) {
			url = this.getClass().getResource(configurationResourceName);
		}
		if (LOG.isDebugEnabled()) {
			LOG.debug("Creating EhCacheProvider from a specified resource: " + configurationResourceName
					+ " Resolved to URL: " + url);
		}
		return url;
	}

	public void stop() {
		if (this.manager != null) {
			this.manager.shutdown();
			this.manager = null;
		}
	}
}
