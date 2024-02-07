package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStream;

import org.hl7.fhir.r4.model.Resource;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;

public interface ThymeleafTemplateService
{
	void writeTo(Resource resource, Class<?> type, MediaType mediaType, UriInfo uriInfo,
			SecurityContext securityContext, OutputStream outputStream) throws IOException;
}
