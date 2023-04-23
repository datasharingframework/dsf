package dev.dsf.bpe.plugin;

import java.nio.file.Path;

import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.core.env.ConfigurableEnvironment;

import ca.uhn.fhir.context.FhirContext;

public interface ProcessPluginFactory<D, L extends TaskListener>
{
	int getApiVersion();

	Class<D> getProcessPluginDefinitionType();

	ProcessPlugin<D, ?, L> createProcessPlugin(D processPluginDefinition, boolean draft, Path jarFile,
			ClassLoader classLoader, FhirContext fhirContext, ConfigurableEnvironment environment);
}