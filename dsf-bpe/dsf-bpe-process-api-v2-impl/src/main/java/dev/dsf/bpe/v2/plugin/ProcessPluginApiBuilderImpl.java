package dev.dsf.bpe.v2.plugin;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import dev.dsf.bpe.api.plugin.ProcessPluginApiBuilder;
import dev.dsf.bpe.api.plugin.ProcessPluginFactory;
import dev.dsf.bpe.v2.spring.ApiServiceConfig;

public class ProcessPluginApiBuilderImpl implements ProcessPluginApiBuilder
{
	@Override
	public ProcessPluginFactory build(ClassLoader apiClassLoader, ApplicationContext apiApplicationContext,
			ConfigurableEnvironment environment, String serverBaseUrl)
	{
		return new ProcessPluginFactoryImpl(apiClassLoader, apiApplicationContext, environment, serverBaseUrl);
	}

	@Override
	public Class<?> getSpringServiceConfigClass()
	{
		return ApiServiceConfig.class;
	}
}
