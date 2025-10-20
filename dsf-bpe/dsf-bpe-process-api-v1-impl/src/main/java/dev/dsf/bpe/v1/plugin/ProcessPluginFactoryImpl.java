package dev.dsf.bpe.v1.plugin;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.operaton.bpm.engine.impl.variable.serializer.TypedValueSerializer;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import dev.dsf.bpe.api.listener.ListenerFactory;
import dev.dsf.bpe.api.plugin.AbstractProcessPluginFactory;
import dev.dsf.bpe.api.plugin.ProcessPlugin;
import dev.dsf.bpe.api.plugin.ProcessPluginFactory;
import dev.dsf.bpe.v1.ProcessPluginDefinition;

public class ProcessPluginFactoryImpl extends AbstractProcessPluginFactory implements ProcessPluginFactory
{
	public static final int API_VERSION = 1;

	public ProcessPluginFactoryImpl(ClassLoader apiClassLoader, ApplicationContext apiApplicationContext,
			ConfigurableEnvironment environment, String serverBaseUrl)
	{
		super(API_VERSION, apiClassLoader, apiApplicationContext, environment, serverBaseUrl,
				ProcessPluginDefinition.class);
	}

	@Override
	protected URLClassLoader createPluginClassLoader(String name, URL[] urls, ClassLoader parent)
	{
		return new ProcessPluginClassLoader(name, urls, parent);
	}

	@Override
	protected ProcessPlugin createProcessPlugin(Object processPluginDefinition, boolean draft, Path jarFile,
			ClassLoader pluginClassLoader)
	{
		return new ProcessPluginImpl((ProcessPluginDefinition) processPluginDefinition, API_VERSION, draft, jarFile,
				pluginClassLoader, environment, apiApplicationContext, serverBaseUrl);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Stream<TypedValueSerializer> getSerializer()
	{
		return apiApplicationContext.getBeansOfType(TypedValueSerializer.class).values().stream();
	}

	@Override
	public ListenerFactory getListenerFactory()
	{
		return apiApplicationContext.getBean(ListenerFactory.class);
	}
}