package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.QuestionnaireResponse;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.QuestionnaireResponseDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.webservice.specification.QuestionnaireResponseService;

public class QuestionnaireResponseServiceSecure extends
		AbstractResourceServiceSecure<QuestionnaireResponseDao, QuestionnaireResponse, QuestionnaireResponseService>
		implements QuestionnaireResponseService
{
	public QuestionnaireResponseServiceSecure(QuestionnaireResponseService delegate, String serverBase,
			ResponseGenerator responseGenerator, ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, QuestionnaireResponseDao QuestionnaireResponseDao,
			ExceptionHandler exceptionHandler, ParameterConverter parameterConverter,
			AuthorizationRule<QuestionnaireResponse> authorizationRule, ResourceValidator resourceValidator)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				QuestionnaireResponse.class, QuestionnaireResponseDao, exceptionHandler, parameterConverter,
				authorizationRule, resourceValidator);
	}
}
