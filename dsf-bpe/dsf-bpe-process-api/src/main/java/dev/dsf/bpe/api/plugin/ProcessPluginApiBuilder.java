package dev.dsf.bpe.api.plugin;

import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

public interface ProcessPluginApiBuilder
{
	ProcessPluginFactory build(ClassLoader apiClassLoader, ApplicationContext apiApplicationContext,
			ConfigurableEnvironment environment);

	Class<?> getSpringServiceConfigClass();
}
