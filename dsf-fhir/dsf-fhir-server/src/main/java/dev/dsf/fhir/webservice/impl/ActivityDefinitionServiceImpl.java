package dev.dsf.fhir.webservice.impl;

import org.hl7.fhir.r4.model.ActivityDefinition;

import dev.dsf.fhir.authorization.AuthorizationRuleProvider;
import dev.dsf.fhir.dao.ActivityDefinitionDao;
import dev.dsf.fhir.event.EventGenerator;
import dev.dsf.fhir.event.EventHandler;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.history.HistoryService;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.ActivityDefinitionService;

public class ActivityDefinitionServiceImpl extends
		AbstractResourceServiceImpl<ActivityDefinitionDao, ActivityDefinition> implements ActivityDefinitionService
{
	public ActivityDefinitionServiceImpl(String path, String serverBase, int defaultPageCount,
			ActivityDefinitionDao dao, ResourceValidator validator, EventHandler eventHandler,
			ExceptionHandler exceptionHandler, EventGenerator eventGenerator, ResponseGenerator responseGenerator,
			ParameterConverter parameterConverter, ReferenceExtractor referenceExtractor,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			AuthorizationRuleProvider authorizationRuleProvider, HistoryService historyService,
			ValidationRules validationRules)
	{
		super(path, ActivityDefinition.class, serverBase, defaultPageCount, dao, validator, eventHandler,
				exceptionHandler, eventGenerator, responseGenerator, parameterConverter, referenceExtractor,
				referenceResolver, referenceCleaner, authorizationRuleProvider, historyService, validationRules);
	}
}
