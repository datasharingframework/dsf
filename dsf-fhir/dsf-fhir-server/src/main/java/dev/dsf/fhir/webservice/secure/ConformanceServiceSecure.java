package dev.dsf.fhir.webservice.secure;

import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.webservice.specification.ConformanceService;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public class ConformanceServiceSecure extends AbstractServiceSecure<ConformanceService> implements ConformanceService
{
	public ConformanceServiceSecure(ConformanceService delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver);
	}

	@Override
	public Response getMetadata(String mode, UriInfo uri, HttpHeaders headers)
	{
		// get metadata allowed for all authenticated users

		return delegate.getMetadata(mode, uri, headers);
	}
}
