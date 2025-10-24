package dev.dsf.fhir.webservice.specification;

import java.io.InputStream;

import org.hl7.fhir.r4.model.Binary;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public interface BinaryService extends BasicResourceService<Binary>
{
	Response create(InputStream in, UriInfo uri, HttpHeaders headers);

	Response update(String id, InputStream in, UriInfo uri, HttpHeaders headers);

	Response readHead(String id, UriInfo uri, HttpHeaders headers);

	Response vreadHead(String id, long version, UriInfo uri, HttpHeaders headers);
}
