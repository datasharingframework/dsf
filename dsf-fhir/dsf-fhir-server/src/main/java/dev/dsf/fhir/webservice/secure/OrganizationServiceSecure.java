package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.Organization;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.OrganizationDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.OrganizationService;

public class OrganizationServiceSecure extends
		AbstractResourceServiceSecure<OrganizationDao, Organization, OrganizationService> implements OrganizationService
{
	public OrganizationServiceSecure(OrganizationService delegate, String serverBase,
			ResponseGenerator responseGenerator, ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, OrganizationDao organizationDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<Organization> authorizationRule,
			ResourceValidator resourceValidator, ValidationRules validationRules)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				Organization.class, organizationDao, exceptionHandler, parameterConverter, authorizationRule,
				resourceValidator, validationRules);
	}
}
