package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.PractitionerRole;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.PractitionerRoleDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.DefaultProfileProvider;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.PractitionerRoleService;

public class PractitionerRoleServiceSecure
		extends AbstractResourceServiceSecure<PractitionerRoleDao, PractitionerRole, PractitionerRoleService>
		implements PractitionerRoleService
{
	public PractitionerRoleServiceSecure(PractitionerRoleService delegate, String serverBase,
			ResponseGenerator responseGenerator, ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, PractitionerRoleDao practitionerRoleDao,
			ExceptionHandler exceptionHandler, ParameterConverter parameterConverter,
			AuthorizationRule<PractitionerRole> authorizationRule, ResourceValidator resourceValidator,
			ValidationRules validationRules, DefaultProfileProvider defaultProfileProvider)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				PractitionerRole.class, practitionerRoleDao, exceptionHandler, parameterConverter, authorizationRule,
				resourceValidator, validationRules, defaultProfileProvider);
	}
}
