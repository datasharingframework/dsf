package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.ValueSet;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.ValueSetDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.webservice.specification.ValueSetService;

public class ValueSetServiceSecure extends AbstractResourceServiceSecure<ValueSetDao, ValueSet, ValueSetService>
		implements ValueSetService
{
	public ValueSetServiceSecure(ValueSetService delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, ValueSetDao valueSetDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<ValueSet> authorizationRule,
			ResourceValidator resourceValidator)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				ValueSet.class, valueSetDao, exceptionHandler, parameterConverter, authorizationRule,
				resourceValidator);
	}
}
