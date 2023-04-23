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
import dev.dsf.bpe.v1.activity.DefaultUserTaskListener;

public class ProcessPluginFactoryImpl
		implements ProcessPluginFactory<ProcessPluginDefinition, DefaultUserTaskListener>, InitializingBean
{
	private final ProcessPluginApi processPluginApi;
	private final DefaultUserTaskListener defaultUserTaskListener;

	public ProcessPluginFactoryImpl(ProcessPluginApi processPluginApi, DefaultUserTaskListener defaultUserTaskListener)
	{
		this.processPluginApi = processPluginApi;
		this.defaultUserTaskListener = defaultUserTaskListener;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(processPluginApi, "processPluginApi");
		Objects.requireNonNull(defaultUserTaskListener, "defaultUserTaskListener");
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
	public ProcessPlugin<ProcessPluginDefinition, ?, DefaultUserTaskListener> createProcessPlugin(
			ProcessPluginDefinition processPluginDefinition, boolean draft, Path jarFile, ClassLoader classLoader,
			FhirContext fhirContext, ConfigurableEnvironment environment)
	{
		return new ProcessPluginImpl(processPluginDefinition, processPluginApi, defaultUserTaskListener, draft, jarFile,
				classLoader, fhirContext, environment);
	}
}
