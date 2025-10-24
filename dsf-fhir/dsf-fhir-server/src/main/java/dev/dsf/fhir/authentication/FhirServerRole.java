package dev.dsf.fhir.authentication;

import java.util.List;

import org.hl7.fhir.r4.model.ResourceType;

import dev.dsf.common.auth.conf.DsfRole;

public interface FhirServerRole extends DsfRole
{
	List<ResourceType> resourceTypes();
}
