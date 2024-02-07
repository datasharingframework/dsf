package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Objects;

import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.InitializingBean;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Produces(MediaType.TEXT_HTML)
public class ThymeleafAdapter implements MessageBodyWriter<Resource>, InitializingBean
{
	@Context
	private volatile UriInfo uriInfo;

	@Context
	private volatile SecurityContext securityContext;

	private final ThymeleafTemplateService thymeleafTemplateService;

	public ThymeleafAdapter(ThymeleafTemplateService thymeleafTemplateService)
	{
		this.thymeleafTemplateService = thymeleafTemplateService;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(thymeleafTemplateService, "thymeleafTemplateService");
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
	{
		return type != null && Resource.class.isAssignableFrom(type);
	}

	@Override
	public void writeTo(Resource resource, Class<?> type, Type genericType, Annotation[] annotations,
			MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException
	{
		thymeleafTemplateService.writeTo(resource, type, mediaType, uriInfo, securityContext, entityStream);
	}
}
