package dev.dsf.fhir.adapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.codec.binary.Base64OutputStream;
import org.hl7.fhir.r4.model.BaseResource;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;

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
	public static final String PRETTY = "pretty";
	public static final String SUMMARY = "summary";

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
			if ("true".equals(mediaType.getParameters().getOrDefault(PRETTY, "false")))
				p.setPrettyPrint(true);

			switch (mediaType.getParameters().getOrDefault(SUMMARY, "false"))
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
		if (t instanceof Binary b && b.getDataElement() instanceof DeferredBase64BinaryType d)
			writeBinary(mediaType, entityStream, b, d);
		else if (t instanceof Bundle b && getDeferredBase64BinaryTypes(b).findAny().isPresent())
			writeBundleWithBinary(mediaType, entityStream, b);
		else
			getParser(mediaType).encodeResourceToWriter(t, new OutputStreamWriter(entityStream));
	}

	private void writeBinary(MediaType mediaType, OutputStream entityStream, Binary binary,
			DeferredBase64BinaryType data) throws IOException
	{
		final String placeholder = data.createPlaceHolderAndSetAsUserData();

		String s = getParser(mediaType).encodeResourceToString(binary);

		OutputStreamWriter writer = new OutputStreamWriter(entityStream);
		if (s.contains(placeholder))
		{
			String[] split = s.split(placeholder);
			if (split.length == 2)
			{
				writer.write(split[0]);
				writer.flush();

				Base64OutputStream base64 = new Base64OutputStream(entityStream, true, Integer.MIN_VALUE, null);
				data.writeExternal(base64);
				base64.eof();
				base64.flush();

				s = split[1];
			}
			else
				throw new RuntimeException("Bad binary data placeholder");
		}

		writer.write(s);
		writer.flush();
	}

	private Stream<DeferredBase64BinaryType> getDeferredBase64BinaryTypes(Bundle bundle)
	{
		return bundle.getEntry().stream().filter(BundleEntryComponent::hasResource)
				.map(BundleEntryComponent::getResource).flatMap(r ->
				{
					if (r instanceof Bundle b)
						return getDeferredBase64BinaryTypes(b);
					else if (r instanceof Binary b && b.getDataElement() instanceof DeferredBase64BinaryType d)
						return Stream.of(d);
					else
						return null;
				}).filter(Objects::nonNull);
	}

	private void writeBundleWithBinary(MediaType mediaType, OutputStream entityStream, Bundle bundle) throws IOException
	{
		Map<String, DeferredBase64BinaryType> dataByPlaceholder = getDeferredBase64BinaryTypes(bundle)
				.collect(Collectors.toMap(DeferredBase64BinaryType::createPlaceHolderAndSetAsUserData,
						Function.identity(), (a, b) -> a, LinkedHashMap::new));

		String s = getParser(mediaType).encodeResourceToString(bundle);

		OutputStreamWriter writer = new OutputStreamWriter(entityStream);
		for (Entry<String, DeferredBase64BinaryType> e : dataByPlaceholder.entrySet())
		{
			final String placeholder = e.getKey();
			final DeferredBase64BinaryType data = e.getValue();

			if (s.contains(placeholder))
			{
				String[] split = s.split(placeholder);
				if (split.length == 2)
				{
					writer.write(split[0]);
					writer.flush();

					Base64OutputStream base64 = new Base64OutputStream(entityStream, true, Integer.MIN_VALUE, null);
					data.writeExternal(base64);
					base64.eof();
					base64.flush();

					s = split[1];
				}
				else
					throw new RuntimeException("Bad binary data placeholder");
			}
		}

		writer.write(s);
		writer.flush();
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
		BaseResource resource = getParser(mediaType).parseResource(type, new InputStreamReader(entityStream));

		// HAPI FHIR parser adds contained resources to bundle references
		if (resource instanceof Bundle b)
			resource = referenceCleaner.cleanReferenceResourcesIfBundle(b);

		return resource;
	}
}
