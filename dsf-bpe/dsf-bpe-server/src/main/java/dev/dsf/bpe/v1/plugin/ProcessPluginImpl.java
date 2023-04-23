package dev.dsf.bpe.v1.plugin;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.springframework.core.env.ConfigurableEnvironment;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.bpe.plugin.AbstractProcessPlugin;
import dev.dsf.bpe.plugin.ProcessPlugin;
import dev.dsf.bpe.v1.ProcessPluginApi;
import dev.dsf.bpe.v1.ProcessPluginDefinition;
import dev.dsf.bpe.v1.activity.DefaultUserTaskListener;

public class ProcessPluginImpl
		extends AbstractProcessPlugin<ProcessPluginDefinition, ProcessPluginApi, DefaultUserTaskListener>
		implements ProcessPlugin<ProcessPluginDefinition, ProcessPluginApi, DefaultUserTaskListener>
{
	public ProcessPluginImpl(ProcessPluginDefinition processPluginDefinition, ProcessPluginApi processPluginApi,
			DefaultUserTaskListener defaultUserTaskListener, boolean draft, Path jarFile, ClassLoader classLoader,
			FhirContext fhirContext, ConfigurableEnvironment environment)
	{
		super(processPluginDefinition, processPluginApi, defaultUserTaskListener, draft, jarFile, classLoader,
				fhirContext, environment);
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
	protected void registerApiBeans(BiConsumer<String, Object> registry)
	{
		registry.accept("processPluginApi", getProcessPluginApi());
		registry.accept("endpointProvider", getProcessPluginApi().getEndpointProvider());
		registry.accept("fhirContext", getProcessPluginApi().getFhirContext());
		registry.accept("fhirWebserviceClientProvider", getProcessPluginApi().getFhirWebserviceClientProvider());
		registry.accept("mailService", getProcessPluginApi().getMailService());
		registry.accept("objectMapper", getProcessPluginApi().getObjectMapper());
		registry.accept("organizationProvider", getProcessPluginApi().getOrganizationProvider());
		registry.accept("questionnaireResponseHelper", getProcessPluginApi().getQuestionnaireResponseHelper());
		registry.accept("taskHelper", getProcessPluginApi().getTaskHelper());
	}

	@Override
	protected String getProcessPluginApiVersion()
	{
		return "1";
	}

	@Override
	protected Optional<String> getLocalOrganizationIdentifierValue()
	{
		return getProcessPluginApi().getOrganizationProvider().getLocalOrganizationIdentifierValue();
	}
}
