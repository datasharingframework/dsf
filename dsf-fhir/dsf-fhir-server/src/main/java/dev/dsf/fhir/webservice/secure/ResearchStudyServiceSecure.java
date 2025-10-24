package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.ResearchStudy;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.ResearchStudyDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.ResearchStudyService;

public class ResearchStudyServiceSecure
		extends AbstractResourceServiceSecure<ResearchStudyDao, ResearchStudy, ResearchStudyService>
		implements ResearchStudyService
{
	public ResearchStudyServiceSecure(ResearchStudyService delegate, String serverBase,
			ResponseGenerator responseGenerator, ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, ResearchStudyDao researchStudyDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<ResearchStudy> authorizationRule,
			ResourceValidator resourceValidator, ValidationRules validationRules)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				ResearchStudy.class, researchStudyDao, exceptionHandler, parameterConverter, authorizationRule,
				resourceValidator, validationRules);
	}
}
