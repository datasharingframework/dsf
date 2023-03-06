package dev.dsf.fhir.webservice.specification;

import org.hl7.fhir.r4.model.Bundle;

import dev.dsf.fhir.webservice.base.BasicService;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public interface RootService extends BasicService
{
	Response root(UriInfo uri, HttpHeaders headers);

	Response history(UriInfo uri, HttpHeaders headers);

	Response handleBundle(Bundle bundle, UriInfo uri, HttpHeaders headers);
}
