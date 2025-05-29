package dev.dsf.fhir.webservice.secure;

import java.io.InputStream;

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
import dev.dsf.fhir.validation.ValidationRules;
import dev.dsf.fhir.webservice.specification.BinaryService;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public class BinaryServiceSecure extends AbstractResourceServiceSecure<BinaryDao, Binary, BinaryService>
		implements BinaryService
{
	public BinaryServiceSecure(BinaryService delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver, ReferenceCleaner referenceCleaner,
			ReferenceExtractor referenceExtractor, BinaryDao binaryDao, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, AuthorizationRule<Binary> authorizationRule,
			ResourceValidator resourceValidator, ValidationRules validationRules)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver, referenceCleaner, referenceExtractor,
				Binary.class, binaryDao, exceptionHandler, parameterConverter, authorizationRule, resourceValidator,
				validationRules);
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

	@Override
	public Response readHead(String id, UriInfo uri, HttpHeaders headers)
	{
		Response read = delegate.readHead(id, uri, headers);

		return checkRead(read);
	}

	@Override
	public Response vreadHead(String id, long version, UriInfo uri, HttpHeaders headers)
	{
		Response read = delegate.vreadHead(id, version, uri, headers);

		return checkRead(read);
	}
}