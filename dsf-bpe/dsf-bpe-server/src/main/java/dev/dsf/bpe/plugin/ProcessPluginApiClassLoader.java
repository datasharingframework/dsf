/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.bpe.plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessPluginApiClassLoader extends URLClassLoader
{
	static
	{
		ClassLoader.registerAsParallelCapable();
	}

	private static final Logger logger = LoggerFactory.getLogger(ProcessPluginApiClassLoader.class);

	private final Set<String> allowedBpeClasses = new HashSet<>();
	private final Set<String> resourcesWithPriority = new HashSet<>();
	private final Set<String> allowedBpeResources = new HashSet<>();

	public ProcessPluginApiClassLoader(String name, URL[] urls, ClassLoader bpeLoader, Set<String> allowedBpeClasses,
			Set<String> resourcesWithPriority, Set<String> allowedBpeResources)
	{
		super(name, urls, bpeLoader);

		if (allowedBpeClasses != null)
			this.allowedBpeClasses.addAll(allowedBpeClasses);

		if (resourcesWithPriority != null)
			this.resourcesWithPriority.addAll(resourcesWithPriority);

		if (allowedBpeResources != null)
			this.allowedBpeResources.addAll(allowedBpeResources);
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException
	{
		return loadClass(name, false);
	}

	@Override
	protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException
	{
		synchronized (getClassLoadingLock(className))
		{
			// check already loaded
			Class<?> apiClass = findLoadedClass(className);
			if (apiClass != null)
				return apiClass;

			// check api class path
			apiClass = loadClassAsResource(className);
			if (apiClass != null)
				return apiClass;

			// check bpe
			Class<?> bpeClass = getParent().loadClass(className);
			if (isBpeClassAllowed(bpeClass))
				return bpeClass;

			logger.debug("Class " + className + " not found or hidden");
			throw new ClassNotFoundException(className);
		}
	}

	private Class<?> loadClassAsResource(final String name) throws ClassNotFoundException
	{
		URL apiClassUrl = findResource(toClassReference(name));
		if (apiClassUrl != null)
		{
			Class<?> apiClass = findClass(name);
			resolveClass(apiClass);

			return apiClass;
		}
		else
			return null;
	}

	private String toClassReference(String className)
	{
		return className == null ? null : className.replace('.', '/').concat(".class");
	}

	@Override
	public URL getResource(String name)
	{
		URL resource = null;

		URL apiResourceUrl = findResource(name);
		if (apiResourceUrl != null && hasApiResourcePriority(name, apiResourceUrl))
			resource = apiResourceUrl;
		else
		{
			URL bpeResourceUrl = getParent().getResource(name);
			if (bpeResourceUrl != null && isBpeResourceAllowed(name, bpeResourceUrl))
				resource = bpeResourceUrl;
			else if (apiResourceUrl != null)
				resource = apiResourceUrl;
		}

		if (resource == null && name.startsWith("/"))
			resource = getResource(name.substring(1));

		return resource;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException
	{
		List<URL> fromBpe = new ArrayList<>(), fromApi = new ArrayList<>();

		Enumeration<URL> urls = getParent().getResources(name);
		while (urls != null && urls.hasMoreElements())
		{
			URL bpeResourceUrl = urls.nextElement();
			if (isBpeResourceAllowed(name, bpeResourceUrl))
				fromBpe.add(bpeResourceUrl);
		}

		urls = findResources(name);
		while (urls != null && urls.hasMoreElements())
		{
			URL apiResourceUrl = urls.nextElement();
			if (hasApiResourcePriority(name, apiResourceUrl) || fromBpe.isEmpty())
				fromApi.add(apiResourceUrl);
		}

		fromApi.addAll(fromBpe);

		return Collections.enumeration(fromApi);
	}

	/**
	 * @param clazz
	 * @return <code>false</code> if bpe class should be hidden from api or process plugin
	 */
	private boolean isBpeClassAllowed(Class<?> clazz)
	{
		final String className = clazz.getName();
		final String packageName = clazz.getPackageName();

		if (className.startsWith("java.") || className.startsWith("javax.") || allowedBpeClasses.contains(className)
				|| isPackageAllowed(packageName))
			return true;

		logger.debug("{}: BPE class {} not allowed", getName(), className);
		return false;
	}

	private boolean isPackageAllowed(String packageName)
	{
		if (allowedBpeClasses.contains(packageName))
			return true;

		String[] split = packageName.split("\\.");
		StringBuilder b = new StringBuilder(packageName);
		for (int s = split.length - 1; s > 1; s--)
		{
			b.delete(b.length() - split[s].length() - 1, b.length() + split[s].length() + 1);
			if (allowedBpeClasses.contains(b.toString()))
				return true;
		}

		return false;
	}

	/**
	 * @param name
	 * @param apiResourceUrl
	 * @return <code>true</code> if resource from api or process plugins has priority over resource from bpe
	 */
	private boolean hasApiResourcePriority(String name, URL apiResourceUrl)
	{
		if ("jar".equals(apiResourceUrl.getProtocol()) && resourcesWithPriority.contains(name))
			return true;

		logger.debug("{}: BPE resource {} / {} has priority", getName(), name, apiResourceUrl);
		return false;
	}

	/**
	 * @param name
	 * @param bpeResourcetUrl
	 * @return <code>false</code> if resource from bpe should be hidden from api or process plugins
	 */
	private boolean isBpeResourceAllowed(String name, URL bpeResourcetUrl)
	{
		if ("jar".equals(bpeResourcetUrl.getProtocol()) && allowedBpeResources.contains(name))
			return true;

		logger.debug("{}: BPE resource {} / {} not allowed", getName(), name, bpeResourcetUrl);
		return false;
	}
}
