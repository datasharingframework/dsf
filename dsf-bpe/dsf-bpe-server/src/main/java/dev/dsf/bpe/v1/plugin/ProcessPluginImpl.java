package dev.dsf.bpe.v1.plugin;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.core.env.ConfigurableEnvironment;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.plugin.AbstractProcessPlugin;
import dev.dsf.bpe.plugin.ProcessPlugin;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.ProcessPluginDefinition;

public class ProcessPluginImpl extends AbstractProcessPlugin<ProcessPluginDefinition, ProcessPluginApi>
		implements ProcessPlugin<ProcessPluginDefinition, ProcessPluginApi>
{
	public ProcessPluginImpl(ProcessPluginDefinition processPluginDefinition, ProcessPluginApi processPluginApi,
			boolean draft, Path jarFile, ClassLoader classLoader, FhirContext fhirContext,
			ConfigurableEnvironment environment)
	{
		super(processPluginDefinition, processPluginApi, draft, jarFile, classLoader, fhirContext, environment);
	}

	@Override
	protected List<Class<?>> getDefinitionSpringConfigurations()
	{
		return getProcessPluginDefinition().getSpringConfigurations();
	}

	@Override
	protected String getDefinitionName()
	{
		return getProcessPluginDefinition().getName();
	}

	@Override
	protected String getDefinitionVersion()
	{
		return getProcessPluginDefinition().getVersion();
	}

	@Override
	protected String getDefinitionResourceVersion()
	{
		return getProcessPluginDefinition().getResourceVersion();
	}

	@Override
	protected LocalDate getDefinitionReleaseDate()
	{
		return getProcessPluginDefinition().getReleaseDate();
	}

	@Override
	protected LocalDate getDefinitionResourceReleaseDate()
	{
		return getProcessPluginDefinition().getResourceReleaseDate();
	}

	@Override
	protected Map<String, List<String>> getDefinitionFhirResourcesByProcessId()
	{
		return getProcessPluginDefinition().getFhirResourcesByProcessId();
	}

	@Override
	protected List<String> getDefinitionProcessModels()
	{
		return getProcessPluginDefinition().getProcessModels();
	}

	@Override
	protected Class<?> getDefaultSpringConfiguration()
	{
		return DefaultSpringConfiguration.class;
	}

	@Override
	protected String getProcessPluginApiVersion()
	{
		return "1";
	}
}
