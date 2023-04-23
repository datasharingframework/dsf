package dev.dsf.fhir.webservice.jaxrs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.api.Constants;
import dev.dsf.fhir.webservice.specification.ConformanceService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path(ConformanceServiceJaxrs.PATH)
@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
		Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
public class ConformanceServiceJaxrs extends AbstractServiceJaxrs<ConformanceService> implements ConformanceService
{
	public static final String PATH = "metadata";

	private static final Logger logger = LoggerFactory.getLogger(ConformanceServiceJaxrs.class);

	public ConformanceServiceJaxrs(ConformanceService delegate)
	{
		super(delegate);
	}

	@GET
	@Override
	public Response getMetadata(@QueryParam("mode") String mode, @Context UriInfo uri, @Context HttpHeaders headers)
	{
		logger.trace("GET {}", uri.getRequestUri().toString());

		return delegate.getMetadata(mode, uri, headers);
	}
}
