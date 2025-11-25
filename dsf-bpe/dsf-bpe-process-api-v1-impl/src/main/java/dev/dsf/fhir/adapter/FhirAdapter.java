/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.function.Supplier;

import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.Constants;
import dev.dsf.fhir.service.ReferenceCleaner;
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
public class FhirAdapter implements MessageBodyReader<BaseResource>, MessageBodyWriter<BaseResource>
{
	private final FhirContext fhirContext;
	private final ReferenceCleaner referenceCleaner;

	public FhirAdapter(FhirContext fhirContext, ReferenceCleaner referenceCleaner)
	{
		this.fhirContext = fhirContext;
		this.referenceCleaner = referenceCleaner;
	}

	private IParser getParser(MediaType mediaType, Supplier<IParser> parserFactor)
	{
		/* Parsers are not guaranteed to be thread safe */
		IParser p = parserFactor.get();
		p.setStripVersionsFromReferences(false);
		p.setOverrideResourceIdWithBundleEntryFullUrl(false);

		if (mediaType != null)
		{
			if ("true".equals(mediaType.getParameters().getOrDefault("pretty", "false")))
				p.setPrettyPrint(true);

			switch (mediaType.getParameters().getOrDefault("summary", "false"))
			{
				case "true" -> p.setSummaryMode(true);
				case "text" -> p.setEncodeElements(Set.of("*.text", "*.id", "*.meta", "*.(mandatory)"));
				case "data" -> p.setSuppressNarratives(true);
			}
		}

		return p;
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
		return switch (resource)
		{
			case Bundle b -> fixBundle(b);
			case Binary b -> fixBinary(b);
			default -> resource;
		};
	}

	private BaseResource fixBundle(Bundle resource)
	{
		// HAPI FHIR parser adds contained resources to bundle references
		resource = referenceCleaner.cleanReferenceResourcesIfBundle(resource);

		if (resource.hasIdElement() && resource.getIdElement().hasIdPart()
				&& !resource.getIdElement().hasVersionIdPart() && resource.hasMeta()
				&& resource.getMeta().hasVersionId())
		{
			// Bugfix HAPI 5.1.0 is removing version information from bundle.id
			IdType fixedId = new IdType(resource.getResourceType().name(), resource.getIdElement().getIdPart(),
					resource.getMeta().getVersionId());
			resource.setIdElement(fixedId);
		}

		// Bugfix HAPI 5.1.0 is removing version information from bundle.id
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
			// Bugfix HAPI 5.1.0 is removing version information from binary.id
			IdType fixedId = new IdType(resource.getResourceType().name(), resource.getIdElement().getIdPart(),
					resource.getMeta().getVersionId());
			resource.setIdElement(fixedId);
		}

		return resource;
	}
}
