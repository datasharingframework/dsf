package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.Bundle;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.BundleDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.BundleService;

public class BundleServiceSecure extends AbstractResourceServiceSecure<BundleDao, Bundle, BundleService>
		implements BundleService
{
	public BundleServiceSecure(BundleService delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, BundleDao bundleDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<Bundle> authorizationRule,
			ResourceValidator resourceValidator, ValidationRules validationRules)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				Bundle.class, bundleDao, exceptionHandler, parameterConverter, authorizationRule, resourceValidator,
				validationRules);
	}
}
