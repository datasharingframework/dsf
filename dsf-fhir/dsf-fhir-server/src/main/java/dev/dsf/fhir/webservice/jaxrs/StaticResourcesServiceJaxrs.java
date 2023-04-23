package dev.dsf.fhir.webservice.jaxrs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.api.Constants;
import dev.dsf.fhir.webservice.specification.StaticResourcesService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path(StaticResourcesServiceJaxrs.PATH)
@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
		Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
public class StaticResourcesServiceJaxrs extends AbstractServiceJaxrs<StaticResourcesService>
		implements StaticResourcesService
{
	public static final String PATH = "static";

	private static final Logger logger = LoggerFactory.getLogger(StaticResourcesServiceJaxrs.class);

	public StaticResourcesServiceJaxrs(StaticResourcesService delegate)
	{
		super(delegate);
	}

	@GET
	@Path("/{fileName}")
	@Override
	public Response getFile(@PathParam("fileName") String fileName, @Context UriInfo uri, @Context HttpHeaders headers)
	{
		logger.trace("GET {}", uri.getRequestUri().toString());

		return delegate.getFile(fileName, uri, headers);
	}
}
