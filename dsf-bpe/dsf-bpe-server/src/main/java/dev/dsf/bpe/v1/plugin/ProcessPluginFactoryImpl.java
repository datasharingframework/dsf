package dev.dsf.bpe.v1.plugin;

import java.nio.file.Path;
import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.env.ConfigurableEnvironment;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.plugin.ProcessPlugin;
import dev.dsf.bpe.plugin.ProcessPluginFactory;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.ProcessPluginDefinition;

public class ProcessPluginFactoryImpl implements ProcessPluginFactory<ProcessPluginDefinition>, InitializingBean
{
	private final ProcessPluginApi processPluginApi;

	public ProcessPluginFactoryImpl(ProcessPluginApi processPluginApi)
	{
		this.processPluginApi = processPluginApi;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(processPluginApi, "processPluginApi");
	}

	@Override
	public int getApiVersion()
	{
		return 1;
	}

	@Override
	public Class<ProcessPluginDefinition> getProcessPluginDefinitionType()
	{
		return ProcessPluginDefinition.class;
	}

	@Override
	public ProcessPlugin<ProcessPluginDefinition, ?> createProcessPlugin(
			ProcessPluginDefinition processPluginDefinition, boolean draft, Path jarFile, ClassLoader classLoader,
			FhirContext fhirContext, ConfigurableEnvironment environment)
	{
		return new ProcessPluginImpl(processPluginDefinition, processPluginApi, draft, jarFile, classLoader,
				fhirContext, environment);
	}
}
