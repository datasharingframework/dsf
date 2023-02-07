package dev.dsf.bpe.service;

import java.util.List;
import java.util.Map;

import dev.dsf.bpe.plugin.ProcessPluginDefinitionAndClassLoader;
import dev.dsf.bpe.process.ProcessKeyAndVersion;
import dev.dsf.bpe.process.ProcessStateChangeOutcome;

public interface FhirResourceHandler
{
	void applyStateChangesAndStoreNewResourcesInDb(
			Map<ProcessKeyAndVersion, ProcessPluginDefinitionAndClassLoader> definitionByProcessKeyAndVersion,
			List<ProcessStateChangeOutcome> changes);
}
