package dev.dsf.bpe.plugin;

import java.nio.file.Path;

import org.springframework.core.env.ConfigurableEnvironment;

import ca.uhn.fhir.context.FhirContext;

public interface ProcessPluginFactory<D>
{
	int getApiVersion();

	Class<D> getProcessPluginDefinitionType();

	ProcessPlugin<D, ?> createProcessPlugin(D processPluginDefinition, boolean draft, Path jarFile,
			ClassLoader classLoader, FhirContext fhirContext, ConfigurableEnvironment environment);
}