package dev.dsf.bpe.v1.plugin;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import dev.dsf.bpe.api.plugin.ProcessPluginApiBuilder;
import dev.dsf.bpe.api.plugin.ProcessPluginFactory;
import dev.dsf.bpe.v1.spring.ApiServiceConfig;

public class ProcessPluginApiBuilderImpl implements ProcessPluginApiBuilder
{
	@Override
	public ProcessPluginFactory build(ClassLoader apiClassLoader, ApplicationContext apiApplicationContext,
			ConfigurableEnvironment environment)
	{
		return new ProcessPluginFactoryImpl(apiClassLoader, apiApplicationContext, environment);
	}

	@Override
	public Class<?> getSpringServiceConfigClass()
	{
		return ApiServiceConfig.class;
	}
}
