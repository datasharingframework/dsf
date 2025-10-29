package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.Practitioner;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.PractitionerDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.DefaultProfileProvider;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.PractitionerService;

public class PractitionerServiceSecure extends
		AbstractResourceServiceSecure<PractitionerDao, Practitioner, PractitionerService> implements PractitionerService
{
	public PractitionerServiceSecure(PractitionerService delegate, String serverBase,
			ResponseGenerator responseGenerator, ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, PractitionerDao practitionerDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<Practitioner> authorizationRule,
			ResourceValidator resourceValidator, ValidationRules validationRules, DefaultProfileProvider defaultProfileProvider)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				Practitioner.class, practitionerDao, exceptionHandler, parameterConverter, authorizationRule,
				resourceValidator, validationRules, defaultProfileProvider);
	}
}
