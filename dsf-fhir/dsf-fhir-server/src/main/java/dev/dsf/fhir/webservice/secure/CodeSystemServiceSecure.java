package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.CodeSystem;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.CodeSystemDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.CodeSystemService;

public class CodeSystemServiceSecure extends AbstractResourceServiceSecure<CodeSystemDao, CodeSystem, CodeSystemService>
		implements CodeSystemService
{
	public CodeSystemServiceSecure(CodeSystemService delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, CodeSystemDao codeSystemDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<CodeSystem> authorizationRule,
			ResourceValidator resourceValidator, ValidationRules validationRules)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				CodeSystem.class, codeSystemDao, exceptionHandler, parameterConverter, authorizationRule,
				resourceValidator, validationRules);
	}
}
