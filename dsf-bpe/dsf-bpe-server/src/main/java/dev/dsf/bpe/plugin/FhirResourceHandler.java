package dev.dsf.bpe.plugin;

import java.util.List;
import java.util.Map;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;

public interface FhirResourceHandler
{
	void applyStateChangesAndStoreNewResourcesInDb(Map<ProcessIdAndVersion, List<byte[]>> resources,
			List<ProcessStateChangeOutcome> changes);
}
