package dev.dsf.fhir.dao.command;

import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.common.auth.conf.Identity;

public interface ValidationHelper
{
	ValidationResult checkResourceValidForCreate(Identity identity, Resource resource);

	ValidationResult checkResourceValidForUpdate(Identity identity, Resource resource);
}
