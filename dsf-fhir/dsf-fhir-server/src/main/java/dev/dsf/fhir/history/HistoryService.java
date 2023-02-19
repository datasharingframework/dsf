package dev.dsf.fhir.history;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.common.auth.Identity;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

public interface HistoryService
{
	Bundle getHistory(Identity identity, UriInfo uri, HttpHeaders headers);

	Bundle getHistory(Identity identity, UriInfo uri, HttpHeaders headers, Class<? extends Resource> resource);

	Bundle getHistory(Identity identity, UriInfo uri, HttpHeaders headers, Class<? extends Resource> resource,
			String id);
}
