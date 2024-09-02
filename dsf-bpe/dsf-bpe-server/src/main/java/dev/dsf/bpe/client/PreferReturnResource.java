package dev.dsf.bpe.client;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

public interface PreferReturnResource
{
	<R extends Resource> R update(R resource);

	Bundle postBundle(Bundle bundle);
}