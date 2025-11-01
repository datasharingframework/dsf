package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.ActivityDefinition;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.ActivityDefinitionDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.DefaultProfileProvider;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.ActivityDefinitionService;

public class ActivityDefinitionServiceSecure
		extends AbstractResourceServiceSecure<ActivityDefinitionDao, ActivityDefinition, ActivityDefinitionService>
		implements ActivityDefinitionService
{
	public ActivityDefinitionServiceSecure(ActivityDefinitionService delegate, String serverBase,
			ResponseGenerator responseGenerator, ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, ActivityDefinitionDao activityDefinitionDao,
			ExceptionHandler exceptionHandler, ParameterConverter parameterConverter,
			AuthorizationRule<ActivityDefinition> authorizationRule, ResourceValidator resourceValidator,
			ValidationRules validationRules, DefaultProfileProvider defaultProfileProvider)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				ActivityDefinition.class, activityDefinitionDao, exceptionHandler, parameterConverter,
				authorizationRule, resourceValidator, validationRules, defaultProfileProvider);
	}
}
