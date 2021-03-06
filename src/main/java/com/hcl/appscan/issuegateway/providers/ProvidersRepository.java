/**
 * © Copyright IBM Corporation 2018.
 * © Copyright HCL Technologies Ltd. 2018. 
 * LICENSE: Apache License, Version 2.0 https://www.apache.org/licenses/LICENSE-2.0
 */
package com.hcl.appscan.issuegateway.providers;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import common.IProvider;
import groovy.lang.GroovyClassLoader;

// For now, when a new provider is added the service would need to be restarted. 
// Would want to avoid that with perhaps a directoryWatcher + reload function here
@Component
public class ProvidersRepository {
	
	private static Map<String, IProvider> providers = null;
	
    private static final Logger logger = LoggerFactory.getLogger(ProvidersRepository.class);
    
	public synchronized static Map<String,IProvider> getProviders(List<String> errors) {
		if (providers == null) {
			providers = new HashMap<String, IProvider>();
			File providersRoot = new File(System.getProperty("providers.path", "."));
			try {
			for (File providerPath : getSubFolders(providersRoot)) {
				File providerGroovy = getFirstProvider(providerPath);
				if (providerGroovy == null) {
					continue;
				}
				GroovyClassLoader classLoader = new GroovyClassLoader();
				classLoader.addClasspath(providersRoot.getAbsolutePath());
				IProvider provider = (IProvider)classLoader.parseClass(providerGroovy).newInstance();
			    providers.put(provider.getId(), provider);
			    classLoader.close();
			}
			} catch (Exception e) {
				errors.add("Internal Server Error: Problem loading providers : " + e.getMessage());
				logger.error("Internal Server Error while loading providers", e);
			}
		}
		return providers;
	}
	
	private static File[] getSubFolders(File parent) {
		return parent.listFiles(new FilenameFilter() {
		  @Override
		  public boolean accept(File current, String name) {
		    return new File(current, name).isDirectory() && !name.equals("common");
		  }
		});
	}
	
	private static File getFirstProvider(File parent) {
		File[] children = parent.listFiles(new FilenameFilter() {
		  @Override
		  public boolean accept(File current, String name) {
		    return name.endsWith("Provider.groovy") && new File(current, name).isFile();
		  }
		});
		if (children.length > 0) {
			return children[0];
		}
		return null;
	}
}
