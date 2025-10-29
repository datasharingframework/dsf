package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.OrganizationAffiliation;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.OrganizationAffiliationDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.DefaultProfileProvider;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.OrganizationAffiliationService;

public class OrganizationAffiliationServiceSecure extends
		AbstractResourceServiceSecure<OrganizationAffiliationDao, OrganizationAffiliation, OrganizationAffiliationService>
		implements OrganizationAffiliationService
{
	public OrganizationAffiliationServiceSecure(OrganizationAffiliationService delegate, String serverBase,
			ResponseGenerator responseGenerator, ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, OrganizationAffiliationDao organizationDao,
			ExceptionHandler exceptionHandler, ParameterConverter parameterConverter,
			AuthorizationRule<OrganizationAffiliation> authorizationRule, ResourceValidator resourceValidator,
			ValidationRules validationRules, DefaultProfileProvider defaultProfileProvider)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				OrganizationAffiliation.class, organizationDao, exceptionHandler, parameterConverter, authorizationRule,
				resourceValidator, validationRules, defaultProfileProvider);
	}
}
