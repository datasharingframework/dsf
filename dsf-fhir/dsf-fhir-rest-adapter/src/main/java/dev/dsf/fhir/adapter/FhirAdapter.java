package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.Constants;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.MessageBodyReader;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Consumes({ Constants.CT_FHIR_XML_NEW, Constants.CT_FHIR_XML, MediaType.APPLICATION_XML, MediaType.TEXT_XML,
		Constants.CT_FHIR_JSON_NEW, Constants.CT_FHIR_JSON, MediaType.APPLICATION_JSON })
@Produces({ Constants.CT_FHIR_XML_NEW, Constants.CT_FHIR_XML, MediaType.APPLICATION_XML, MediaType.TEXT_XML,
		Constants.CT_FHIR_JSON_NEW, Constants.CT_FHIR_JSON, MediaType.APPLICATION_JSON })
public class FhirAdapter extends AbstractAdapter
		implements MessageBodyReader<BaseResource>, MessageBodyWriter<BaseResource>
{
	private final FhirContext fhirContext;

	public FhirAdapter(FhirContext fhirContext)
	{
		this.fhirContext = fhirContext;
	}

	private IParser getParser(MediaType mediaType)
	{
		return switch (mediaType.getType() + "/" + mediaType.getSubtype())
		{
			case Constants.CT_FHIR_XML_NEW, Constants.CT_FHIR_XML, MediaType.APPLICATION_XML, MediaType.TEXT_XML ->
				getParser(mediaType, fhirContext::newXmlParser);
			case Constants.CT_FHIR_JSON_NEW, Constants.CT_FHIR_JSON, MediaType.APPLICATION_JSON ->
				getParser(mediaType, fhirContext::newJsonParser);
			default -> throw new IllegalStateException("MediaType " + mediaType.toString() + " not supported");
		};
	}

	@Override
	public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
	{
		return type != null && BaseResource.class.isAssignableFrom(type);
	}

	@Override
	public void writeTo(BaseResource t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
			MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream)
			throws IOException, WebApplicationException
	{
		getParser(mediaType).encodeResourceToWriter(t, new OutputStreamWriter(entityStream));
	}

	@Override
	public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
	{
		return type != null && BaseResource.class.isAssignableFrom(type);
	}

	@Override
	public BaseResource readFrom(Class<BaseResource> type, Type genericType, Annotation[] annotations,
			MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
			throws IOException, WebApplicationException
	{
		return fixResource(getParser(mediaType).parseResource(type, new InputStreamReader(entityStream)));
	}

	private BaseResource fixResource(BaseResource resource)
	{
		if (resource instanceof Bundle)
			return fixBundle((Bundle) resource);
		else if (resource instanceof Binary)
			return fixBinary((Binary) resource);
		else
			return resource;
	}

	private BaseResource fixBundle(Bundle resource)
	{
		if (resource.hasIdElement() && resource.getIdElement().hasIdPart()
				&& !resource.getIdElement().hasVersionIdPart() && resource.hasMeta()
				&& resource.getMeta().hasVersionId())
		{
			// TODO Bugfix HAPI is removing version information from bundle.id
			IdType fixedId = new IdType(resource.getResourceType().name(), resource.getIdElement().getIdPart(),
					resource.getMeta().getVersionId());
			resource.setIdElement(fixedId);
		}

		// TODO Bugfix HAPI is removing version information from bundle.id
		resource.getEntry().stream().filter(e -> e.hasResource() && e.getResource() instanceof Bundle)
				.map(e -> (Bundle) e.getResource()).forEach(this::fixResource);

		return resource;
	}

	private BaseResource fixBinary(Binary resource)
	{
		if (resource.hasIdElement() && resource.getIdElement().hasIdPart()
				&& !resource.getIdElement().hasVersionIdPart() && resource.hasMeta()
				&& resource.getMeta().hasVersionId())
		{
			// TODO Bugfix HAPI is removing version information from binary.id
			IdType fixedId = new IdType(resource.getResourceType().name(), resource.getIdElement().getIdPart(),
					resource.getMeta().getVersionId());
			resource.setIdElement(fixedId);
		}

		return resource;
	}
}
