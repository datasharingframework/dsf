package dev.dsf.fhir.webservice.secure;

import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.webservice.specification.StaticResourcesService;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public class StaticResourcesServiceSecure extends AbstractServiceSecure<StaticResourcesService>
		implements StaticResourcesService
{
	public StaticResourcesServiceSecure(StaticResourcesService delegate, String serverBase,
			ResponseGenerator responseGenerator, ReferenceResolver referenceResolver)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver);
	}

	@Override
	public Response getFile(String fileName, UriInfo uri, HttpHeaders headers)
	{
		logCurrentIdentity();

		// get static files allowed for all authenticated users

		return delegate.getFile(fileName, uri, headers);
	}
}
