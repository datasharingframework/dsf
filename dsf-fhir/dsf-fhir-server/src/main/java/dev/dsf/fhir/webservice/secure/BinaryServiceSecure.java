package dev.dsf.fhir.webservice.secure;

import java.io.InputStream;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.hl7.fhir.r4.model.Binary;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.dao.BinaryDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.ResourceValidator;
import dev.dsf.fhir.webservice.specification.BinaryService;

public class BinaryServiceSecure extends AbstractResourceServiceSecure<BinaryDao, Binary, BinaryService>
		implements BinaryService
{
	public BinaryServiceSecure(BinaryService delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, BinaryDao binaryDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<Binary> authorizationRule,
			ResourceValidator resourceValidator)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				Binary.class, binaryDao, exceptionHandler, parameterConverter, authorizationRule, resourceValidator);
	}

	@Override
	public Response create(InputStream in, UriInfo uri, HttpHeaders headers)
	{
		throw new UnsupportedOperationException("Implemented and delegated by jaxrs layer");
	}

	@Override
	public Response update(String id, InputStream in, UriInfo uri, HttpHeaders headers)
	{
		throw new UnsupportedOperationException("Implemented and delegated by jaxrs layer");
	}
}