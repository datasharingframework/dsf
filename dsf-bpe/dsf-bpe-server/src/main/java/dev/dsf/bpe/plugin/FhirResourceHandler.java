package dev.dsf.bpe.plugin;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Resource;

public interface FhirResourceHandler
{
	void applyStateChangesAndStoreNewResourcesInDb(Map<ProcessIdAndVersion, List<Resource>> resources,
			List<ProcessStateChangeOutcome> changes);
}
