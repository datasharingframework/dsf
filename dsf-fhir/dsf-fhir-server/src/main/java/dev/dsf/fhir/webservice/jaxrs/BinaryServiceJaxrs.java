package dev.dsf.fhir.webservice.jaxrs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.hl7.fhir.r4.model.Base64BinaryType;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.rest.api.Constants;
import dev.dsf.fhir.adapter.DeferredBase64BinaryType;
import dev.dsf.fhir.adapter.FhirAdapter;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.model.StreamableBase64BinaryType;
import dev.dsf.fhir.webservice.RangeRequest;
import dev.dsf.fhir.webservice.specification.BinaryService;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.StreamingOutput;
import jakarta.ws.rs.core.UriInfo;

@Path(BinaryServiceJaxrs.PATH)
public class BinaryServiceJaxrs extends AbstractResourceServiceJaxrs<Binary, BinaryService> implements BinaryService
{
	public static final String PATH = "Binary";

	private static final Logger logger = LoggerFactory.getLogger(BinaryServiceJaxrs.class);

	private static final String[] FHIR_MEDIA_TYPES = { Constants.CT_FHIR_XML_NEW, Constants.CT_FHIR_JSON_NEW,
			Constants.CT_FHIR_XML, Constants.CT_FHIR_JSON };

	private static final class BinaryJaxrsOutputStream implements StreamingOutput
	{
		final Binary binary;

		BinaryJaxrsOutputStream(Binary binary)
		{
			this.binary = binary;
		}

		@Override
		public void write(OutputStream output) throws IOException, WebApplicationException
		{
			try (output)
			{
				if (binary.getDataElement() instanceof DeferredBase64BinaryType s)
					s.writeExternal(output);
				else
					new ByteArrayInputStream(binary.getData()).transferTo(output);
			}
		}
	}

	private final ParameterConverter parameterConverter;
	private final FhirAdapter fhirAdapter;

	public BinaryServiceJaxrs(BinaryService delegate, ParameterConverter parameterConverter, FhirAdapter fhirAdapter)
	{
		super(delegate);

		this.parameterConverter = parameterConverter;
		this.fhirAdapter = fhirAdapter;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(parameterConverter, "parameterConverter");
		Objects.requireNonNull(fhirAdapter, "fhirAdapter");
	}

	@POST
	@Consumes({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON })
	@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
	@Override
	public Response create(Binary resource, @Context UriInfo uri, @Context HttpHeaders headers)
	{
		resource.setDataElement(resource.getData() == null ? null : new StreamableBase64BinaryType(resource.getData()));

		return delegate.create(resource, uri, headers);
	}

	@POST
	@Consumes
	@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
	@Override
	public Response create(InputStream in, @Context UriInfo uri, @Context HttpHeaders headers)
	{
		try (in)
		{
			String securityContext = getSecurityContext(headers);
			String contentType = getContentType(headers);

			Binary resource = createBinary(contentType, in, securityContext);
			return delegate.create(resource, uri, headers);
		}
		catch (IOException e)
		{
			throw new WebApplicationException(e);
		}
	}

	private Binary createBinary(String contentType, InputStream inputStream, String securityContextReference)
	{
		Binary resource = new Binary();
		resource.setContentType(contentType);
		resource.setDataElement(inputStream == null ? null : new StreamableBase64BinaryType(inputStream));
		resource.setSecurityContext(new Reference(securityContextReference));
		return resource;
	}

	private String getSecurityContext(HttpHeaders headers)
	{
		return getHeaderValueOrThrowBadRequest(headers, Constants.HEADER_X_SECURITY_CONTEXT);
	}

	private String getContentType(HttpHeaders headers)
	{
		return getHeaderValueOrThrowBadRequest(headers, HttpHeaders.CONTENT_TYPE);
	}

	private String getHeaderValueOrThrowBadRequest(HttpHeaders headers, String header)
	{
		List<String> headerValue = headers.getRequestHeader(header);
		if (headerValue != null && headerValue.size() == 1)
		{
			String hV0 = headerValue.get(0);
			if (hV0 != null && !hV0.isBlank())
				return hV0;
			else
			{
				logger.warn("{} header found, no value, sending {}", header, Status.BAD_REQUEST);
				throw new WebApplicationException(Status.BAD_REQUEST);
			}
		}
		else if (headerValue != null && headerValue.size() > 1)
		{
			logger.warn("{} header found, more than one value, sending {}", header, Status.BAD_REQUEST);
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		headerValue = headers.getRequestHeader(header.toLowerCase());
		if (headerValue != null && headerValue.size() == 1)
		{
			String hV0 = headerValue.get(0);
			if (hV0 != null && !hV0.isBlank())
				return hV0;
			else
			{
				logger.warn("{} header found, no value, sending {}", header, Status.BAD_REQUEST);
				throw new WebApplicationException(Status.BAD_REQUEST);
			}
		}
		else if (headerValue != null && headerValue.size() > 1)
		{
			logger.warn("{} header found, more than one value, sending {}", header, Status.BAD_REQUEST);
			throw new WebApplicationException(Status.BAD_REQUEST);
		}

		logger.warn("{} header not found, sending {}", header, Status.BAD_REQUEST);
		throw new WebApplicationException(Status.BAD_REQUEST);
	}

	@GET
	@Path("/{id}")
	@Produces
	@Override
	public Response read(@PathParam("id") String id, @Context UriInfo uri, @Context HttpHeaders headers)
	{
		Response read = super.read(id, uri, headers);

		return configureReadResponse(uri, headers, false, read);
	}

	@GET
	@Path("/{id}/_history/{version}")
	@Produces
	@Override
	public Response vread(@PathParam("id") String id, @PathParam("version") long version, @Context UriInfo uri,
			@Context HttpHeaders headers)
	{
		Response read = super.vread(id, version, uri, headers);

		return configureReadResponse(uri, headers, false, read);
	}

	@HEAD
	@Path("/{id}")
	@Produces
	@Override
	public Response readHead(@PathParam("id") String id, @Context UriInfo uri, @Context HttpHeaders headers)
	{
		Response read = delegate.readHead(id, uri, headers);

		return configureReadResponse(uri, headers, true, read);
	}

	@HEAD
	@Path("/{id}/_history/{version}")
	@Produces
	@Override
	public Response vreadHead(@PathParam("id") String id, @PathParam("version") long version, @Context UriInfo uri,
			@Context HttpHeaders headers)
	{
		Response read = delegate.vreadHead(id, version, uri, headers);

		return configureReadResponse(uri, headers, true, read);
	}

	private Response configureReadResponse(UriInfo uri, HttpHeaders headers, boolean head, Response read)
	{
		Optional<MediaType> fhirMediaType = getValidFhirMediaType(uri, headers);

		if (read.getEntity() instanceof Binary binary && fhirMediaType.isEmpty())
		{
			if (mediaTypeMatches(headers, binary))
			{
				long dataSize = (long) binary.getUserData(RangeRequest.USER_DATA_VALUE_DATA_SIZE);

				if (head)
				{
					return toStreamResponse(binary).header(HttpHeaders.CONTENT_LENGTH, dataSize).build();
				}
				else
				{
					RangeRequest rangeRequest = (RangeRequest) binary
							.getUserData(RangeRequest.USER_DATA_VALUE_RANGE_REQUEST);

					if (rangeRequest != null && !rangeRequest.isRangeSatisfiable(dataSize))
					{
						return Response.status(Status.REQUESTED_RANGE_NOT_SATISFIABLE)
								.header(RangeRequest.CONTENT_RANGE_HEADER,
										rangeRequest.createContentRangeHeaderValue(dataSize))
								.entity("").build();
						// empty string as content to not trigger default error handler and override header
						// alternative: configure jersey.config.server.response.setStatusOverSendError = true
						// via JettyServer webAppContext.getServletContext().setAttribute ...
					}

					ResponseBuilder response = toStreamResponse(binary);

					// if range request
					if (rangeRequest != null && !rangeRequest.isRangeNotDefined())
					{
						response = response.status(Status.PARTIAL_CONTENT)
								.header(RangeRequest.CONTENT_RANGE_HEADER,
										rangeRequest.createRangeHeaderValue(dataSize))
								.header(HttpHeaders.CONTENT_LENGTH, rangeRequest.getRequestedLength(dataSize));
					}
					else
						response = response.header(HttpHeaders.CONTENT_LENGTH, dataSize);

					return response.entity(new BinaryJaxrsOutputStream(binary)).build();
				}
			}
			else
				return Response.status(Status.NOT_ACCEPTABLE).build();
		}
		else if (read.getEntity() instanceof Binary binary && fhirMediaType.isPresent() && head)
		{
			ResponseBuilder b = Response.status(Status.OK);
			b.type(fhirMediaType.get());

			if (binary.getMeta() != null && binary.getMeta().getLastUpdated() != null
					&& binary.getMeta().getVersionId() != null)
			{
				b.lastModified(binary.getMeta().getLastUpdated());
				b.tag(new EntityTag(binary.getMeta().getVersionId(), true));
			}

			b.cacheControl(ResponseGenerator.PRIVATE_NO_CACHE_NO_TRANSFORM);

			b.header(HttpHeaders.CONTENT_LENGTH, calculateFhirResponseSize(binary, fhirMediaType.get()));

			return b.build();
		}
		else
			return read;
	}

	private long calculateFhirResponseSize(Binary binary, MediaType mediaType)
	{
		long dataSize = (long) binary.getUserData(RangeRequest.USER_DATA_VALUE_DATA_SIZE);

		// setting single byte to make sure data element is part of xml/json
		binary.setDataElement(new Base64BinaryType(new byte[1]));

		try (CountingOutputStream out = new CountingOutputStream(NullOutputStream.INSTANCE))
		{
			fhirAdapter.writeTo(binary, Binary.class, null, null, mediaType, null, out);

			// minus 4 to account for single byte in data element
			return out.getByteCount() - 4 + calculateBase64EncodedLength(dataSize);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e);
		}
	}

	private long calculateBase64EncodedLength(long dataSize)
	{
		return dataSize < 0 ? 0 : 4 * ((dataSize + 2) / 3);
	}

	private Optional<MediaType> getValidFhirMediaType(UriInfo uri, HttpHeaders headers)
	{
		// _format parameter override present and valid
		if (uri.getQueryParameters().containsKey(Constants.PARAM_FORMAT))
		{
			MediaType mediaType = parameterConverter.getMediaTypeThrowIfNotSupported(uri, headers);
			return Optional.of(mediaType);
		}
		else
		{
			List<MediaType> types = headers.getAcceptableMediaTypes();
			MediaType accept = types == null ? null : types.get(0);

			// accept header is FHIR mime-type
			return Arrays.stream(FHIR_MEDIA_TYPES).filter(f -> f.equals(accept.toString())).findFirst()
					.map(_ -> accept);
		}
	}

	private boolean mediaTypeMatches(HttpHeaders headers, Binary binary)
	{
		MediaType binaryMediaType = MediaType.valueOf(binary.getContentType());
		return headers.getAcceptableMediaTypes() != null && headers.getAcceptableMediaTypes().stream()
				.anyMatch(acceptType -> acceptType.isCompatible(binaryMediaType));
	}

	private ResponseBuilder toStreamResponse(Binary binary)
	{
		ResponseBuilder b = Response.status(Status.OK);
		b.type(binary.getContentType() != null ? binary.getContentType() : MediaType.APPLICATION_OCTET_STREAM);

		if (binary.getMeta() != null && binary.getMeta().getLastUpdated() != null
				&& binary.getMeta().getVersionId() != null)
		{
			b.lastModified(binary.getMeta().getLastUpdated());
			b.tag(new EntityTag(binary.getMeta().getVersionId(), true));
		}

		if (binary.hasSecurityContext() && binary.getSecurityContext().hasReference())
		{
			// Not setting header for logical references
			b.header(Constants.HEADER_X_SECURITY_CONTEXT, binary.getSecurityContext().getReference());
		}

		b.cacheControl(ResponseGenerator.PRIVATE_NO_CACHE_NO_TRANSFORM);
		b.header(RangeRequest.ACCEPT_RANGES_HEADER, RangeRequest.ACCEPT_RANGES_HEADER_VALUE);
		b.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + toFileName(binary));

		return b;
	}

	private String toFileName(Binary binary)
	{
		return "Binary_" + binary.getIdElement().getIdPart() + switch (binary.getContentType())
		{
			case "image/jpeg" -> ".jpeg";
			case "image/png" -> ".png";
			case "image/svg+xml" -> ".svg";
			case "image/tiff" -> ".tiff";
			case "text/csv" -> ".csv";
			case "text/html" -> ".html";
			case "text/plain" -> ".txt";
			case "application/dicom" -> ".dicom";
			case "application/dicom+json" -> ".json";
			case "application/dicom+xml" -> ".xml";
			case "application/gzip" -> ".gz";
			case "application/json" -> ".json";
			case "application/pdf" -> ".pdf";
			case "application/pem-certificate-chain" -> ".pem";
			case "application/x-ndjson" -> ".ndjson";
			case "application/xml" -> ".xml";
			case "application/zip" -> ".zip";
			default -> ".bin";
		};
	}

	@PUT
	@Path("/{id}")
	@Consumes({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON })
	@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
	@Override
	public Response update(@PathParam("id") String id, Binary resource, @Context UriInfo uri,
			@Context HttpHeaders headers)
	{
		resource.setDataElement(resource.getData() == null ? null : new StreamableBase64BinaryType(resource.getData()));

		return delegate.update(id, resource, uri, headers);
	}

	@PUT
	@Consumes({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON })
	@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
	@Override
	public Response update(Binary resource, @Context UriInfo uri, @Context HttpHeaders headers)
	{
		resource.setDataElement(resource.getData() == null ? null : new StreamableBase64BinaryType(resource.getData()));

		return delegate.update(resource, uri, headers);
	}

	@PUT
	@Path("/{id}")
	@Consumes
	@Produces({ Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW, MediaType.APPLICATION_XML, Constants.CT_FHIR_JSON,
			Constants.CT_FHIR_JSON_NEW, MediaType.APPLICATION_JSON, MediaType.TEXT_HTML })
	@Override
	public Response update(@PathParam("id") String id, InputStream in, @Context UriInfo uri,
			@Context HttpHeaders headers)
	{
		try (in)
		{
			String securityContext = getSecurityContext(headers);
			String contentType = getContentType(headers);

			Binary resource = createBinary(contentType, in, securityContext);
			return delegate.update(id, resource, uri, headers);
		}
		catch (IOException e)
		{
			throw new WebApplicationException(e);
		}
	}

	@Override
	public Response search(UriInfo uri, HttpHeaders headers)
	{
		Response response = super.search(uri, headers);

		return response;
	}
}
