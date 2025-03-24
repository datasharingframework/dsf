package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.HealthcareService;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.HealthcareServiceDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.HealthcareServiceService;

public class HealthcareServiceServiceSecure
		extends AbstractResourceServiceSecure<HealthcareServiceDao, HealthcareService, HealthcareServiceService>
		implements HealthcareServiceService
{
	public HealthcareServiceServiceSecure(HealthcareServiceService delegate, String serverBase,
			ResponseGenerator responseGenerator, ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, HealthcareServiceDao healthcareServiceDao,
			ExceptionHandler exceptionHandler, ParameterConverter parameterConverter,
			AuthorizationRule<HealthcareService> authorizationRule, ResourceValidator resourceValidator,
			ValidationRules validationRules)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				HealthcareService.class, healthcareServiceDao, exceptionHandler, parameterConverter, authorizationRule,
				resourceValidator, validationRules);
	}
}
