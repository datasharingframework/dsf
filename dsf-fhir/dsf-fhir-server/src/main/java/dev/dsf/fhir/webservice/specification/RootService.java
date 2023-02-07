package dev.dsf.fhir.webservice.specification;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.hl7.fhir.r4.model.Bundle;

import dev.dsf.fhir.webservice.base.BasicService;

public interface RootService extends BasicService
{
	Response root(UriInfo uri, HttpHeaders headers);

	Response history(UriInfo uri, HttpHeaders headers);

	Response handleBundle(Bundle bundle, UriInfo uri, HttpHeaders headers);
}
