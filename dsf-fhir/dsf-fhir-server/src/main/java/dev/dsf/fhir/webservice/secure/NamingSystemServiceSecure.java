package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.NamingSystem;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.NamingSystemDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.NamingSystemService;

public class NamingSystemServiceSecure extends
		AbstractResourceServiceSecure<NamingSystemDao, NamingSystem, NamingSystemService> implements NamingSystemService
{
	public NamingSystemServiceSecure(NamingSystemService delegate, String serverBase,
			ResponseGenerator responseGenerator, ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, NamingSystemDao naminngSystemDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<NamingSystem> authorizationRule,
			ResourceValidator resourceValidator, ValidationRules validationRules)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				NamingSystem.class, naminngSystemDao, exceptionHandler, parameterConverter, authorizationRule,
				resourceValidator, validationRules);
	}
}
