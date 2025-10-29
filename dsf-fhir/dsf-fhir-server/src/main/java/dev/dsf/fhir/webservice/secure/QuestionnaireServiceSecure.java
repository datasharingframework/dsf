package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.Questionnaire;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.QuestionnaireDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.DefaultProfileProvider;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.QuestionnaireService;

public class QuestionnaireServiceSecure
		extends AbstractResourceServiceSecure<QuestionnaireDao, Questionnaire, QuestionnaireService>
		implements QuestionnaireService
{
	public QuestionnaireServiceSecure(QuestionnaireService delegate, String serverBase,
			ResponseGenerator responseGenerator, ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, QuestionnaireDao questionnaireDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<Questionnaire> authorizationRule,
			ResourceValidator resourceValidator, ValidationRules validationRules,
			DefaultProfileProvider defaultProfileProvider)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				Questionnaire.class, questionnaireDao, exceptionHandler, parameterConverter, authorizationRule,
				resourceValidator, validationRules, defaultProfileProvider);
	}
}
