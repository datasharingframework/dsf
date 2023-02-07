package dev.dsf.fhir.dao.command;

import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.validation.ValidationResult;
import dev.dsf.fhir.authentication.User;

public interface ValidationHelper
{
	ValidationResult checkResourceValidForCreate(User user, Resource resource);

	ValidationResult checkResourceValidForUpdate(User user, Resource resource);
}
