package dev.dsf.bpe.plugin;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessPluginApiClassLoader extends URLClassLoader
{
	private static final Logger logger = LoggerFactory.getLogger(ProcessPluginApiClassLoader.class);

	public ProcessPluginApiClassLoader(String name, URL[] urls, ClassLoader parent)
	{
		super(name, urls, parent);
	}

	private static String toClassReference(String className)
	{
		if (className == null)
			return null;

		String name = className.replace('.', '/').concat(".class");
		return name;
	}

	private Class<?> loadAsResource(final String name, boolean checkSystemResource) throws ClassNotFoundException
	{
		Class<?> webappClass = null;
		URL webappUrl = findResource(toClassReference(name));

		if (webappUrl != null && (!checkSystemResource || !isResourceHidden(name, webappUrl)))
		{
			webappClass = findClass(name);
			resolveClass(webappClass);
		}

		return webappClass;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException
	{
		return loadClass(name, false);
	}

	@Override
	protected Class<?> loadClass(String className, boolean resolve) throws ClassNotFoundException
	{
		// TODO remove
		// logger.trace("loadClass({}, {}) ...", className, resolve);
		synchronized (getClassLoadingLock(className))
		{
			ClassNotFoundException ex = null;
			Class<?> apiClass = findLoadedClass(className);
			if (apiClass != null)
			{
				// TODO remove
				// logger.trace("\t<-1 {}{}", className,
				// (apiClass.getClassLoader() != null ? (" from " + apiClass.getClassLoader().getName()) : ""));
				return apiClass;
			}

			apiClass = loadAsResource(className, true);
			if (apiClass != null)
			{
				// TODO remove
				// logger.trace("\t<-2 {}{}", className,
				// (apiClass.getClassLoader() != null ? (" from " + apiClass.getClassLoader().getName()) : ""));
				return apiClass;
			}

			try
			{
				Class<?> parentClass = getParent().loadClass(className);
				if (!isClassHidden(parentClass))
				{
					// TODO remove
					// logger.trace("\t<-3 {}{}", className,
					// (parentClass.getClassLoader() != null ? (" from " + parentClass.getClassLoader().getName())
					// : ""));
					return parentClass;
				}
			}
			catch (ClassNotFoundException e)
			{
				ex = e;
			}

			apiClass = loadAsResource(className, false);
			if (apiClass != null)
			{
				// TODO remove
				// logger.trace("\t<-4 {}{}", className,
				// (apiClass.getClassLoader() != null ? (" from " + apiClass.getClassLoader().getName()) : ""));
				return apiClass;
			}

			throw ex == null ? new ClassNotFoundException(className) : ex;
		}
	}

	@Override
	public URL getResource(String name)
	{
		URL resource = null;

		URL webappUrl = findResource(name);
		if (webappUrl != null && !isSystemResource(name, webappUrl))
			resource = webappUrl;
		else
		{
			URL parentUrl = getParent().getResource(name);
			if (parentUrl != null && !isServerResource(name, parentUrl))
				resource = parentUrl;
			else if (webappUrl != null)
				resource = webappUrl;
		}

		if (resource == null && name.startsWith("/"))
			resource = getResource(name.substring(1));

		return resource;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException
	{
		List<URL> fromParent = new ArrayList<>(), fromWebapp = new ArrayList<>();

		Enumeration<URL> urls = getParent().getResources(name);
		while (urls != null && urls.hasMoreElements())
		{
			URL url = urls.nextElement();
			if (!isServerResource(name, url))
				fromParent.add(url);
		}

		urls = findResources(name);
		while (urls != null && urls.hasMoreElements())
		{
			URL url = urls.nextElement();
			if (!isSystemResource(name, url) || fromParent.isEmpty())
				fromWebapp.add(url);
		}

		fromWebapp.addAll(fromParent);

		return Collections.enumeration(fromWebapp);
	}

	private boolean isClassHidden(Class<?> clazz)
	{
		if (clazz.getName().startsWith("java.") || clazz.getName().startsWith("javax.mail.")
				|| clazz.getName().startsWith("javax.xml.") || clazz.getName().startsWith("jakarta.ws.rs.")
				|| clazz.getName().startsWith("org.glassfish.jersey.") || clazz.getName().startsWith("org.slf4j.")
				|| clazz.getName().startsWith("com.fasterxml.jackson."))
			return false;

		logger.trace("TODO should class be hidden? {}", clazz.getName());
		// TODO Auto-generated method stub
		return false;
	}

	private boolean isResourceHidden(String name, URL webappUrl)
	{
		if (name.startsWith("org.hl7.fhir.") || name.startsWith("ca.uhn.fhir."))
			return false;

		logger.trace("TODO should resource be hidden? {} {}", name, webappUrl);
		// TODO Auto-generated method stub
		return false;
	}

	private boolean isSystemResource(String name, URL webappUrl)
	{
		logger.trace("TODO should access to (system) resource be restricted? {} {}", name, webappUrl);
		// TODO Auto-generated method stub
		return false;
	}

	private boolean isServerResource(String name, URL parentUrl)
	{
		logger.trace("TODO should access to (server) resource be restriced? {} {}", name, parentUrl);
		// TODO Auto-generated method stub
		return false;
	}
}
