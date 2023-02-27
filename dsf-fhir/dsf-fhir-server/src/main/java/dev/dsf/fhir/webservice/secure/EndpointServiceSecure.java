package dev.dsf.fhir.webservice.secure;

import org.hl7.fhir.r4.model.Endpoint;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.EndpointDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.webservice.specification.EndpointService;

public class EndpointServiceSecure extends AbstractResourceServiceSecure<EndpointDao, Endpoint, EndpointService>
		implements EndpointService
{
	public EndpointServiceSecure(EndpointService delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, EndpointDao endpointDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<Endpoint> authorizationRule,
			ResourceValidator resourceValidator)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				Endpoint.class, endpointDao, exceptionHandler, parameterConverter, authorizationRule,
				resourceValidator);
	}
}
