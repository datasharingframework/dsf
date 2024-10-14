package dev.dsf.bpe.client;

import java.io.InputStream;
import java.security.KeyStore;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.rest.api.Constants;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

public class FhirWebserviceClientJersey extends AbstractJerseyClient implements FhirWebserviceClient
{
	private static final Logger logger = LoggerFactory.getLogger(FhirWebserviceClientJersey.class);

	private final PreferReturnMinimalWithRetry preferReturnMinimal;

	public FhirWebserviceClientJersey(String baseUrl, KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword,
			ObjectMapper objectMapper, String proxySchemeHostPort, String proxyUserName, char[] proxyPassword,
			int connectTimeout, int readTimeout, boolean logRequests, String userAgentValue, FhirContext fhirContext)
	{
		super(baseUrl, trustStore, keyStore, keyStorePassword, objectMapper,
				Collections.singleton(new FhirAdapter(fhirContext)), proxySchemeHostPort, proxyUserName, proxyPassword,
				connectTimeout, readTimeout, logRequests, userAgentValue);

		preferReturnMinimal = new PreferReturnMinimalWithRetryImpl(this);
	}

	private WebApplicationException handleError(Response response)
	{
		try
		{
			OperationOutcome outcome = response.readEntity(OperationOutcome.class);
			String message = toString(outcome);

			logger.warn("Request failed, OperationOutcome: {}", message);
			return new WebApplicationException(message, response.getStatus());
		}
		catch (ProcessingException e)
		{
			response.close();

			logger.warn("Request failed: {} - {}", e.getClass().getName(), e.getMessage());
			return new WebApplicationException(e, response.getStatus());
		}
	}

	private String toString(OperationOutcome outcome)
	{
		return outcome == null ? "" : outcome.getIssue().stream().map(this::toString).collect(Collectors.joining("\n"));
	}

	private String toString(OperationOutcomeIssueComponent issue)
	{
		return issue == null ? "" : issue.getSeverity() + " " + issue.getCode() + " " + issue.getDiagnostics();
	}

	private void logStatusAndHeaders(Response response)
	{
		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		logger.debug("HTTP header Location: {}", response.getLocation());
		logger.debug("HTTP header ETag: {}", response.getHeaderString(HttpHeaders.ETAG));
		logger.debug("HTTP header Last-Modified: {}", response.getHeaderString(HttpHeaders.LAST_MODIFIED));
	}

	private PreferReturn toPreferReturn(PreferReturnType returnType, Class<? extends Resource> resourceType,
			Response response)
	{
		return switch (returnType)
		{
			case REPRESENTATION -> PreferReturn.resource(response.readEntity(resourceType));
			case MINIMAL -> PreferReturn.minimal(response.getLocation());
			case OPERATION_OUTCOME -> PreferReturn.outcome(response.readEntity(OperationOutcome.class));
			default ->
				throw new RuntimeException(PreferReturn.class.getName() + " value " + returnType + " not supported");
		};
	}

	@Override
	public PreferReturnMinimalWithRetry withMinimalReturn()
	{
		return preferReturnMinimal;
	}

	PreferReturn create(PreferReturnType returnType, Resource resource)
	{
		Objects.requireNonNull(returnType, "returnType");
		Objects.requireNonNull(resource, "resource");

		Response response = getResource().path(resource.getClass().getAnnotation(ResourceDef.class).name()).request()
				.header(Constants.HEADER_PREFER, returnType.getHeaderValue()).accept(Constants.CT_FHIR_JSON_NEW)
				.post(Entity.entity(resource, Constants.CT_FHIR_JSON_NEW));

		logStatusAndHeaders(response);

		if (Status.CREATED.getStatusCode() == response.getStatus())
			return toPreferReturn(returnType, resource.getClass(), response);
		else
			throw handleError(response);
	}

	PreferReturn createConditionaly(PreferReturnType returnType, Resource resource, String ifNoneExistCriteria)
	{
		Objects.requireNonNull(returnType, "returnType");
		Objects.requireNonNull(resource, "resource");
		Objects.requireNonNull(ifNoneExistCriteria, "ifNoneExistCriteria");

		Response response = getResource().path(resource.getClass().getAnnotation(ResourceDef.class).name()).request()
				.header(Constants.HEADER_PREFER, returnType.getHeaderValue())
				.header(Constants.HEADER_IF_NONE_EXIST, ifNoneExistCriteria).accept(Constants.CT_FHIR_JSON_NEW)
				.post(Entity.entity(resource, Constants.CT_FHIR_JSON_NEW));

		logStatusAndHeaders(response);

		if (Status.CREATED.getStatusCode() == response.getStatus())
			return toPreferReturn(returnType, resource.getClass(), response);
		else
			throw handleError(response);
	}

	PreferReturn createBinary(PreferReturnType returnType, InputStream in, MediaType mediaType,
			String securityContextReference)
	{
		Objects.requireNonNull(returnType, "returnType");
		Objects.requireNonNull(in, "in");
		Objects.requireNonNull(mediaType, "mediaType");
		// securityContextReference may be null

		Builder request = getResource().path("Binary").request().header(Constants.HEADER_PREFER,
				returnType.getHeaderValue());
		if (securityContextReference != null && !securityContextReference.isBlank())
			request = request.header(Constants.HEADER_X_SECURITY_CONTEXT, securityContextReference);
		Response response = request.accept(Constants.CT_FHIR_JSON_NEW).post(Entity.entity(in, mediaType));

		logStatusAndHeaders(response);

		if (Status.CREATED.getStatusCode() == response.getStatus())
			return toPreferReturn(returnType, Binary.class, response);
		else
			throw handleError(response);
	}

	PreferReturn update(PreferReturnType returnType, Resource resource)
	{
		Objects.requireNonNull(returnType, "returnType");
		Objects.requireNonNull(resource, "resource");

		Builder builder = getResource().path(resource.getClass().getAnnotation(ResourceDef.class).name())
				.path(resource.getIdElement().getIdPart()).request()
				.header(Constants.HEADER_PREFER, returnType.getHeaderValue()).accept(Constants.CT_FHIR_JSON_NEW);

		if (resource.getMeta().hasVersionId())
			builder.header(Constants.HEADER_IF_MATCH, new EntityTag(resource.getMeta().getVersionId(), true));

		Response response = builder.put(Entity.entity(resource, Constants.CT_FHIR_JSON_NEW));

		logStatusAndHeaders(response);

		if (Status.OK.getStatusCode() == response.getStatus())
			return toPreferReturn(returnType, resource.getClass(), response);
		else
			throw handleError(response);
	}

	PreferReturn updateConditionaly(PreferReturnType returnType, Resource resource, Map<String, List<String>> criteria)
	{
		Objects.requireNonNull(returnType, "returnType");
		Objects.requireNonNull(resource, "resource");
		Objects.requireNonNull(criteria, "criteria");
		if (criteria.isEmpty())
			throw new IllegalArgumentException("criteria map empty");

		WebTarget target = getResource().path(resource.getClass().getAnnotation(ResourceDef.class).name());

		for (Entry<String, List<String>> entry : criteria.entrySet())
			target = target.queryParam(entry.getKey(), entry.getValue().toArray());

		Builder builder = target.request().accept(Constants.CT_FHIR_JSON_NEW).header(Constants.HEADER_PREFER,
				returnType.getHeaderValue());

		if (resource.getMeta().hasVersionId())
			builder.header(Constants.HEADER_IF_MATCH, new EntityTag(resource.getMeta().getVersionId(), true));

		Response response = builder.put(Entity.entity(resource, Constants.CT_FHIR_JSON_NEW));

		logStatusAndHeaders(response);

		if (Status.CREATED.getStatusCode() == response.getStatus() || Status.OK.getStatusCode() == response.getStatus())
			return toPreferReturn(returnType, resource.getClass(), response);
		else
			throw handleError(response);
	}

	PreferReturn updateBinary(PreferReturnType returnType, String id, InputStream in, MediaType mediaType,
			String securityContextReference)
	{
		Objects.requireNonNull(returnType, "returnType");
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(in, "in");
		Objects.requireNonNull(mediaType, "mediaType");
		// securityContextReference may be null

		Builder request = getResource().path("Binary").path(id).request().header(Constants.HEADER_PREFER,
				returnType.getHeaderValue());
		if (securityContextReference != null && !securityContextReference.isBlank())
			request = request.header(Constants.HEADER_X_SECURITY_CONTEXT, securityContextReference);
		Response response = request.accept(Constants.CT_FHIR_JSON_NEW).put(Entity.entity(in, mediaType));

		logStatusAndHeaders(response);

		if (Status.CREATED.getStatusCode() == response.getStatus())
			return toPreferReturn(returnType, Binary.class, response);
		else
			throw handleError(response);
	}

	Bundle postBundle(PreferReturnType returnType, Bundle bundle)
	{
		Objects.requireNonNull(bundle, "bundle");

		Response response = getResource().request().header(Constants.HEADER_PREFER, returnType.getHeaderValue())
				.accept(Constants.CT_FHIR_JSON_NEW).post(Entity.entity(bundle, Constants.CT_FHIR_JSON_NEW));

		logStatusAndHeaders(response);

		if (Status.OK.getStatusCode() == response.getStatus())
			return response.readEntity(Bundle.class);
		else
			throw handleError(response);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R extends Resource> R update(R resource)
	{
		return (R) update(PreferReturnType.REPRESENTATION, resource).getResource();
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return postBundle(PreferReturnType.REPRESENTATION, bundle);
	}

	@Override
	public Bundle searchWithStrictHandling(Class<? extends Resource> resourceType, Map<String, List<String>> parameters)
	{
		Objects.requireNonNull(resourceType, "resourceType");

		WebTarget target = getResource().path(resourceType.getAnnotation(ResourceDef.class).name());
		if (parameters != null)
		{
			for (Entry<String, List<String>> entry : parameters.entrySet())
				target = target.queryParam(entry.getKey(), entry.getValue().toArray());
		}

		Response response = target.request().header(Constants.HEADER_PREFER, PreferHandlingType.STRICT.getHeaderValue())
				.accept(Constants.CT_FHIR_JSON_NEW).get();

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return response.readEntity(Bundle.class);
		else
			throw handleError(response);
	}

	@Override
	public BasicFhirWebserviceClient withRetry(int nTimes, long delayMillis)
	{
		if (nTimes < 0)
			throw new IllegalArgumentException("nTimes < 0");
		if (delayMillis < 0)
			throw new IllegalArgumentException("delayMillis < 0");

		return new BasicFhirWebserviceCientWithRetryImpl(this, nTimes, delayMillis);
	}

	@Override
	public BasicFhirWebserviceClient withRetryForever(long delayMillis)
	{
		if (delayMillis < 0)
			throw new IllegalArgumentException("delayMillis < 0");

		return new BasicFhirWebserviceCientWithRetryImpl(this, RETRY_FOREVER, delayMillis);
	}
}
