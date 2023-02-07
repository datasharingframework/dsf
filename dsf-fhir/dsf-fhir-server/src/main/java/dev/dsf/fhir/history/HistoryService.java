package dev.dsf.fhir.history;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.authentication.User;

public interface HistoryService
{
	Bundle getHistory(User user, UriInfo uri, HttpHeaders headers);

	Bundle getHistory(User user, UriInfo uri, HttpHeaders headers, Class<? extends Resource> resource);

	Bundle getHistory(User user, UriInfo uri, HttpHeaders headers, Class<? extends Resource> resource, String id);
}
