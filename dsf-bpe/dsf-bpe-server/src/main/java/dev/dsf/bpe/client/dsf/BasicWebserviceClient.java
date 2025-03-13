package dev.dsf.bpe.client.dsf;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

public interface BasicWebserviceClient extends PreferReturnResource
{
	Bundle searchWithStrictHandling(Class<? extends Resource> resourceType, Map<String, List<String>> parameters);
}
