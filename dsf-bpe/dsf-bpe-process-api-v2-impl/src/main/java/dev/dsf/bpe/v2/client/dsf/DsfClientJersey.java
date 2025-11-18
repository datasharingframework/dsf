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
package dev.dsf.bpe.v2.client.dsf;

import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.net.ssl.SSLContext;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.logging.LoggingFeature.Verbosity;
import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.BundleEntryResponseComponent;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.OperationOutcome.OperationOutcomeIssueComponent;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.UriType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.rest.api.Constants;
import dev.dsf.bpe.v2.client.dsf.BinaryInputStream.Range;
import dev.dsf.bpe.v2.client.fhir.ClientConfig.BasicAuthentication;
import dev.dsf.bpe.v2.client.fhir.ClientConfig.BearerAuthentication;
import dev.dsf.bpe.v2.client.fhir.ClientConfig.OidcAuthentication;
import dev.dsf.bpe.v2.service.OidcClientProvider;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.AsyncInvoker;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.InvocationCallback;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.FeatureContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.RuntimeDelegate;

public class DsfClientJersey implements DsfClient
{
	private static final Logger logger = LoggerFactory.getLogger(DsfClientJersey.class);

	private static final java.util.logging.Logger requestDebugLogger;
	static
	{
		requestDebugLogger = java.util.logging.Logger.getLogger(DsfClientJersey.class.getName());
		requestDebugLogger.setLevel(Level.INFO);
	}

	private static final String RFC_7231_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";
	private static final Map<String, Class<?>> RESOURCE_TYPES_BY_NAME = Stream.of(ResourceType.values())
			.filter(type -> !ResourceType.List.equals(type))
			.collect(Collectors.toMap(ResourceType::name, DsfClientJersey::getFhirClass));
	private static final String CONTENT_RANGE_PATTERN_TEXT = "bytes (?<start>\\d+)-(?<end>\\d+)\\/(?<size>\\d+)";
	private static final Pattern CONTENT_RANGE_PATTERN = Pattern.compile(CONTENT_RANGE_PATTERN_TEXT);

	private static final Pattern HTTP_STATUS_PATTERN = Pattern.compile("^[12345][0-9]{2}(?: |$)");

	private static Class<?> getFhirClass(ResourceType type)
	{
		try
		{
			return Class.forName("org.hl7.fhir.r4.model." + type.name());
		}
		catch (ClassNotFoundException e)
		{
			throw new RuntimeException(e);
		}
	}

	private static final class BearerAuthenticationFeature implements Feature
	{
		final Supplier<char[]> tokenProvider;

		BearerAuthenticationFeature(Supplier<char[]> tokenProvider)
		{
			this.tokenProvider = Objects.requireNonNull(tokenProvider, "tokenProvider");
		}

		@Override
		public boolean configure(FeatureContext context)
		{
			context.register(new ClientRequestFilter()
			{
				@Override
				public void filter(ClientRequestContext requestContext) throws IOException
				{
					requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION,
							Constants.HEADER_AUTHORIZATION_VALPREFIX_BEARER + String.valueOf(tokenProvider.get()));
				}
			});

			return true;
		}
	}

	private final Client client;
	private final String baseUrl;

	private final PreferReturnMinimalWithRetry preferReturnMinimal;
	private final PreferReturnOutcomeWithRetry preferReturnOutcome;

	public DsfClientJersey(dev.dsf.bpe.v2.client.fhir.ClientConfig clientConfig, OidcClientProvider oidcClientProvider,
			String userAgent, FhirContext fhirContext, ReferenceCleaner referenceCleaner)
	{
		this(clientConfig.getBaseUrl(), clientConfig.getTrustStore(),
				clientConfig.getCertificateAuthentication() == null ? null
						: clientConfig.getCertificateAuthentication().getKeyStore(),
				clientConfig.getCertificateAuthentication() == null ? null
						: clientConfig.getCertificateAuthentication().getKeyStorePassword(),
				clientConfig.getProxy() == null ? null : clientConfig.getProxy().getUrl(),
				clientConfig.getProxy() == null ? null : clientConfig.getProxy().getUsername(),
				clientConfig.getProxy() == null ? null : clientConfig.getProxy().getPassword(),
				clientConfig.getConnectTimeout(), clientConfig.getReadTimeout(), clientConfig.isDebugLoggingEnabled(),
				userAgent, fhirContext, referenceCleaner, authFeatures(clientConfig, oidcClientProvider));
	}

	private static Stream<Feature> authFeatures(dev.dsf.bpe.v2.client.fhir.ClientConfig clientConfig,
			OidcClientProvider oidcClientProvider)
	{
		BasicAuthentication basicAuth = clientConfig.getBasicAuthentication();
		Feature basicAuthFeature = basicAuth == null ? null
				: HttpAuthenticationFeature.basic(basicAuth.getUsername(), new String(basicAuth.getPassword()));

		BearerAuthentication bearerAuth = clientConfig.getBearerAuthentication();
		Feature bearerAuthFeature = bearerAuth == null ? null : new BearerAuthenticationFeature(bearerAuth::getToken);

		OidcAuthentication oidcAuth = clientConfig.getOidcAuthentication();
		Feature oidcAuthFeature = oidcAuth == null ? null
				: new BearerAuthenticationFeature(oidcClientProvider.getOidcClient(oidcAuth)::getAccessToken);

		return Stream.of(basicAuthFeature, bearerAuthFeature, oidcAuthFeature).filter(Objects::nonNull);
	}

	public DsfClientJersey(String baseUrl, KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword,
			String proxySchemeHostPort, String proxyUserName, char[] proxyPassword, Duration connectTimeout,
			Duration readTimeout, boolean logRequestsAndResponses, String userAgentValue, FhirContext fhirContext,
			ReferenceCleaner referenceCleaner)
	{
		this(baseUrl, trustStore, keyStore, keyStorePassword, proxySchemeHostPort, proxyUserName, proxyPassword,
				connectTimeout, readTimeout, logRequestsAndResponses, userAgentValue, fhirContext, referenceCleaner,
				Stream.of());
	}

	private DsfClientJersey(String baseUrl, KeyStore trustStore, KeyStore keyStore, char[] keyStorePassword,
			String proxySchemeHostPort, String proxyUserName, char[] proxyPassword, Duration connectTimeout,
			Duration readTimeout, boolean logRequestsAndResponses, String userAgentValue, FhirContext fhirContext,
			ReferenceCleaner referenceCleaner, Stream<Feature> authFeatures)
	{
		SSLContext sslContext = null;
		if (trustStore != null && keyStore == null && keyStorePassword == null)
			sslContext = SslConfigurator.newInstance().trustStore(trustStore).createSSLContext();
		else if (trustStore != null && keyStore != null && keyStorePassword != null)
			sslContext = SslConfigurator.newInstance().trustStore(trustStore).keyStore(keyStore)
					.keyStorePassword(keyStorePassword).createSSLContext();

		ClientBuilder builder = ClientBuilder.newBuilder();

		authFeatures.forEach(builder::register);

		if (sslContext != null)
			builder.sslContext(sslContext);

		ClientConfig config = new ClientConfig();
		config.connectorProvider(new ApacheConnectorProvider());
		config.property(ClientProperties.PROXY_URI, proxySchemeHostPort);
		config.property(ClientProperties.PROXY_USERNAME, proxyUserName);
		config.property(ClientProperties.PROXY_PASSWORD, proxyPassword == null ? null : String.valueOf(proxyPassword));
		builder.withConfig(config);

		if (userAgentValue != null && !userAgentValue.isBlank())
			builder.register((ClientRequestFilter) requestContext -> requestContext.getHeaders()
					.add(HttpHeaders.USER_AGENT, userAgentValue));

		builder.readTimeout(readTimeout.toMillis(), TimeUnit.MILLISECONDS).connectTimeout(connectTimeout.toMillis(),
				TimeUnit.MILLISECONDS);

		builder.register(new FhirAdapter(fhirContext, referenceCleaner));

		if (logRequestsAndResponses)
		{
			builder.register(new LoggingFeature(requestDebugLogger, Level.INFO, Verbosity.PAYLOAD_ANY,
					LoggingFeature.DEFAULT_MAX_ENTITY_SIZE));
		}

		client = builder.build();

		this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
		// making sure the root url works, this might be a workaround for a jersey client bug

		preferReturnMinimal = new PreferReturnMinimalWithRetryImpl(this);
		preferReturnOutcome = new PreferReturnOutcomeWithRetryImpl(this);
	}

	private WebTarget getResource()
	{
		return client.target(baseUrl);
	}

	@Override
	public String getBaseUrl()
	{
		return baseUrl;
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

	private <R extends Resource> PreferReturn<R> toPreferReturn(PreferReturnType returnType, Class<R> resourceType,
			Response response)
	{
		return switch (returnType)
		{
			case REPRESENTATION -> PreferReturn.resource(response.readEntity(resourceType));

			case MINIMAL -> {
				response.close();

				String location = response.getLocation() == null ? null : response.getLocation().toString();

				if (location == null)
					location = response.getHeaderString(HttpHeaders.CONTENT_LOCATION);

				yield PreferReturn.minimal(location);
			}

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

	@Override
	public PreferReturnOutcomeWithRetry withOperationOutcomeReturn()
	{
		return preferReturnOutcome;
	}

	<R extends Resource> PreferReturn<R> create(PreferReturnType preferReturnType, Class<R> returnType, R resource)
	{
		Objects.requireNonNull(preferReturnType, "preferReturnType");
		Objects.requireNonNull(returnType, "returnType");
		Objects.requireNonNull(resource, "resource");

		Response response = getResource().path(resource.getClass().getAnnotation(ResourceDef.class).name()).request()
				.header(Constants.HEADER_PREFER, preferReturnType.getHeaderValue()).accept(Constants.CT_FHIR_JSON_NEW)
				.post(Entity.entity(resource, Constants.CT_FHIR_JSON_NEW));

		logStatusAndHeaders(response);

		if (Status.CREATED.getStatusCode() == response.getStatus())
			return toPreferReturn(preferReturnType, returnType, response);
		else
			throw handleError(response);
	}

	<R extends Resource> PreferReturn<R> createConditionaly(PreferReturnType preferReturnType, Class<R> returnType,
			R resource, String ifNoneExistCriteria)
	{
		Objects.requireNonNull(preferReturnType, "preferReturnType");
		Objects.requireNonNull(returnType, "returnType");
		Objects.requireNonNull(resource, "resource");
		Objects.requireNonNull(ifNoneExistCriteria, "ifNoneExistCriteria");

		Response response = getResource().path(resource.getClass().getAnnotation(ResourceDef.class).name()).request()
				.header(Constants.HEADER_PREFER, preferReturnType.getHeaderValue())
				.header(Constants.HEADER_IF_NONE_EXIST, ifNoneExistCriteria).accept(Constants.CT_FHIR_JSON_NEW)
				.post(Entity.entity(resource, Constants.CT_FHIR_JSON_NEW));

		logStatusAndHeaders(response);

		if (Status.CREATED.getStatusCode() == response.getStatus())
			return toPreferReturn(preferReturnType, returnType, response);
		else
			throw handleError(response);
	}

	PreferReturn<Binary> createBinary(PreferReturnType preferReturnType, InputStream in, MediaType mediaType,
			String securityContextReference)
	{
		Objects.requireNonNull(preferReturnType, "preferReturnType");
		Objects.requireNonNull(in, "in");
		Objects.requireNonNull(mediaType, "mediaType");
		// securityContextReference may be null

		Builder request = getResource().path("Binary").request().header(Constants.HEADER_PREFER,
				preferReturnType.getHeaderValue());
		if (securityContextReference != null && !securityContextReference.isBlank())
			request = request.header(Constants.HEADER_X_SECURITY_CONTEXT, securityContextReference);
		Response response = request.accept(Constants.CT_FHIR_JSON_NEW).post(Entity.entity(in, mediaType));

		logStatusAndHeaders(response);

		if (Status.CREATED.getStatusCode() == response.getStatus())
			return toPreferReturn(preferReturnType, Binary.class, response);
		else
			throw handleError(response);
	}

	<R extends Resource> PreferReturn<R> update(PreferReturnType preferReturnType, Class<R> returnType, R resource)
	{
		Objects.requireNonNull(preferReturnType, "preferReturnType");
		Objects.requireNonNull(returnType, "returnType");
		Objects.requireNonNull(resource, "resource");

		Builder builder = getResource().path(resource.getClass().getAnnotation(ResourceDef.class).name())
				.path(resource.getIdElement().getIdPart()).request()
				.header(Constants.HEADER_PREFER, preferReturnType.getHeaderValue()).accept(Constants.CT_FHIR_JSON_NEW);

		if (resource.getMeta().hasVersionId())
			builder.header(Constants.HEADER_IF_MATCH, new EntityTag(resource.getMeta().getVersionId(), true));

		Response response = builder.put(Entity.entity(resource, Constants.CT_FHIR_JSON_NEW));

		logStatusAndHeaders(response);

		if (Status.OK.getStatusCode() == response.getStatus())
			return toPreferReturn(preferReturnType, returnType, response);
		else
			throw handleError(response);
	}

	<R extends Resource> PreferReturn<R> updateConditionaly(PreferReturnType preferReturnType, Class<R> returnType,
			R resource, Map<String, List<String>> criteria)
	{
		Objects.requireNonNull(preferReturnType, "preferReturnType");
		Objects.requireNonNull(returnType, "returnType");
		Objects.requireNonNull(resource, "resource");
		Objects.requireNonNull(criteria, "criteria");
		if (criteria.isEmpty())
			throw new IllegalArgumentException("criteria map empty");

		WebTarget target = getResource().path(resource.getClass().getAnnotation(ResourceDef.class).name());

		for (Entry<String, List<String>> entry : criteria.entrySet())
			target = target.queryParam(entry.getKey(), entry.getValue().toArray());

		Builder builder = target.request().accept(Constants.CT_FHIR_JSON_NEW).header(Constants.HEADER_PREFER,
				preferReturnType.getHeaderValue());

		if (resource.getMeta().hasVersionId())
			builder.header(Constants.HEADER_IF_MATCH, new EntityTag(resource.getMeta().getVersionId(), true));

		Response response = builder.put(Entity.entity(resource, Constants.CT_FHIR_JSON_NEW));

		logStatusAndHeaders(response);

		if (Status.CREATED.getStatusCode() == response.getStatus() || Status.OK.getStatusCode() == response.getStatus())
			return toPreferReturn(preferReturnType, returnType, response);
		else
			throw handleError(response);
	}

	PreferReturn<Binary> updateBinary(PreferReturnType preferReturnType, String id, InputStream in, MediaType mediaType,
			String securityContextReference)
	{
		Objects.requireNonNull(preferReturnType, "preferReturnType");
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(in, "in");
		Objects.requireNonNull(mediaType, "mediaType");
		// securityContextReference may be null

		Builder request = getResource().path("Binary").path(id).request().header(Constants.HEADER_PREFER,
				preferReturnType.getHeaderValue());
		if (securityContextReference != null && !securityContextReference.isBlank())
			request = request.header(Constants.HEADER_X_SECURITY_CONTEXT, securityContextReference);
		Response response = request.accept(Constants.CT_FHIR_JSON_NEW).put(Entity.entity(in, mediaType));

		logStatusAndHeaders(response);

		if (Status.CREATED.getStatusCode() == response.getStatus())
			return toPreferReturn(preferReturnType, Binary.class, response);
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
	public <R extends Resource> R create(R resource)
	{
		return (R) create(PreferReturnType.REPRESENTATION, (Class<R>) resource.getClass(), resource).resource();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R extends Resource> R createConditionaly(R resource, String ifNoneExistCriteria)
	{
		return (R) createConditionaly(PreferReturnType.REPRESENTATION, (Class<R>) resource.getClass(), resource,
				ifNoneExistCriteria).resource();
	}

	@Override
	public Binary createBinary(InputStream in, MediaType mediaType, String securityContextReference)
	{
		return (Binary) createBinary(PreferReturnType.REPRESENTATION, in, mediaType, securityContextReference)
				.resource();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R extends Resource> R update(R resource)
	{
		return (R) update(PreferReturnType.REPRESENTATION, (Class<R>) resource.getClass(), resource).resource();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R extends Resource> R updateConditionaly(R resource, Map<String, List<String>> criteria)
	{
		return (R) updateConditionaly(PreferReturnType.REPRESENTATION, (Class<R>) resource.getClass(), resource,
				criteria).resource();
	}

	@Override
	public Binary updateBinary(String id, InputStream in, MediaType mediaType, String securityContextReference)
	{
		return (Binary) updateBinary(PreferReturnType.REPRESENTATION, id, in, mediaType, securityContextReference)
				.resource();
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return postBundle(PreferReturnType.REPRESENTATION, bundle);
	}

	@Override
	public void delete(Class<? extends Resource> resourceClass, String id)
	{
		Objects.requireNonNull(resourceClass, "resourceClass");
		Objects.requireNonNull(id, "id");

		Response response = getResource().path(resourceClass.getAnnotation(ResourceDef.class).name()).path(id).request()
				.accept(Constants.CT_FHIR_JSON_NEW).delete();

		logStatusAndHeaders(response);

		if (Status.OK.getStatusCode() != response.getStatus()
				&& Status.NO_CONTENT.getStatusCode() != response.getStatus())
			throw handleError(response);
		else
			response.close();
	}

	@Override
	public void deleteConditionaly(Class<? extends Resource> resourceClass, Map<String, List<String>> criteria)
	{
		Objects.requireNonNull(resourceClass, "resourceClass");
		Objects.requireNonNull(criteria, "criteria");
		if (criteria.isEmpty())
			throw new IllegalArgumentException("criteria map empty");

		WebTarget target = getResource().path(resourceClass.getAnnotation(ResourceDef.class).name());

		for (Entry<String, List<String>> entry : criteria.entrySet())
			target = target.queryParam(entry.getKey(), entry.getValue().toArray());

		Response response = target.request().accept(Constants.CT_FHIR_JSON_NEW).delete();

		logStatusAndHeaders(response);

		if (Status.OK.getStatusCode() != response.getStatus()
				&& Status.NO_CONTENT.getStatusCode() != response.getStatus())
			throw handleError(response);
		else
			response.close();
	}

	@Override
	public void deletePermanently(Class<? extends Resource> resourceClass, String id)
	{
		Objects.requireNonNull(resourceClass, "resourceClass");
		Objects.requireNonNull(id, "id");

		Response response = getResource().path(resourceClass.getAnnotation(ResourceDef.class).name()).path(id)
				.path("$permanent-delete").request().accept(Constants.CT_FHIR_JSON_NEW).post(null);

		logStatusAndHeaders(response);

		if (Status.OK.getStatusCode() != response.getStatus())
			throw handleError(response);
		else
			response.close();
	}

	@Override
	public Resource read(String resourceTypeName, String id)
	{
		Objects.requireNonNull(resourceTypeName, "resourceTypeName");
		Objects.requireNonNull(id, "id");
		if (!RESOURCE_TYPES_BY_NAME.containsKey(resourceTypeName))
			throw new IllegalArgumentException("Resource of type " + resourceTypeName + " not supported");

		Response response = getResource().path(resourceTypeName).path(id).request().accept(Constants.CT_FHIR_JSON_NEW)
				.get();

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return (Resource) response.readEntity(RESOURCE_TYPES_BY_NAME.get(resourceTypeName));
		else
			throw handleError(response);
	}

	@Override
	public <R extends Resource> R read(Class<R> resourceType, String id)
	{
		return read(resourceType, id, (R) null);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R extends Resource> R read(R oldValue)
	{
		return read((Class<R>) oldValue.getClass(), oldValue.getIdElement().getIdPart(), oldValue);
	}

	private <R extends Resource> R read(Class<R> resourceType, String id, R oldValue)
	{
		Objects.requireNonNull(resourceType, "resourceType");
		Objects.requireNonNull(id, "id");

		Builder request = getResource().path(resourceType.getAnnotation(ResourceDef.class).name()).path(id).request();

		if (oldValue != null && oldValue.hasMeta())
		{
			if (oldValue.getMeta().hasVersionId())
			{
				EntityTag eTag = new EntityTag(oldValue.getMeta().getVersionIdElement().getValue(), true);
				String eTagValue = RuntimeDelegate.getInstance().createHeaderDelegate(EntityTag.class).toString(eTag);
				request.header(HttpHeaders.IF_NONE_MATCH, eTagValue);
				logger.trace("Sending {} Header with value '{}'", HttpHeaders.IF_NONE_MATCH, eTagValue);
			}

			if (oldValue.getMeta().hasLastUpdated())
			{
				String dateValue = formatRfc7231(oldValue.getMeta().getLastUpdated());
				request.header(HttpHeaders.IF_MODIFIED_SINCE, dateValue);
				logger.trace("Sending {} Header with value '{}'", HttpHeaders.IF_MODIFIED_SINCE, dateValue);
			}
		}

		Response response = request.accept(Constants.CT_FHIR_JSON_NEW).get();

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return response.readEntity(resourceType);
		else if (oldValue != null && oldValue.hasMeta()
				&& (oldValue.getMeta().hasVersionId() || oldValue.getMeta().hasLastUpdated())
				&& Status.NOT_MODIFIED.getStatusCode() == response.getStatus())
			return oldValue;
		else
			throw handleError(response);
	}

	private String formatRfc7231(Date date)
	{
		if (date == null)
			return null;
		else
		{
			SimpleDateFormat dateFormat = new SimpleDateFormat(RFC_7231_FORMAT, Locale.US);
			dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
			return dateFormat.format(date);
		}
	}

	@Override
	public <R extends Resource> boolean exists(Class<R> resourceType, String id)
	{
		Objects.requireNonNull(resourceType, "resourceType");
		Objects.requireNonNull(id, "id");

		Response response = getResource().path(resourceType.getAnnotation(ResourceDef.class).name()).path(id).request()
				.accept(Constants.CT_FHIR_JSON_NEW).head();

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return true;
		else if (Status.NOT_FOUND.getStatusCode() == response.getStatus())
			return false;
		else
			throw handleError(response);
	}

	@Override
	public BinaryInputStream readBinary(String id, MediaType mediaType)
	{
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(mediaType, "mediaType");

		Response response = getResource().path("Binary").path(id).request().accept(mediaType).get();

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return toBinaryInputStream(response);
		else
			throw handleError(response);
	}

	@Override
	public BinaryInputStream readBinary(String id, MediaType mediaType, Long rangeStart, Long rangeEndInclusive,
			Map<String, String> additionalHeaders)
	{
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(mediaType, "mediaType");

		Builder builder = getResource().path("Binary").path(id).request().accept(mediaType);

		String range = getRangeHeader(rangeStart, rangeEndInclusive);
		if (range != null)
			builder = builder.header("Range", range);

		if (additionalHeaders != null)
		{
			for (Entry<String, String> e : additionalHeaders.entrySet())
				builder = builder.header(e.getKey(), e.getValue());
		}

		Response response = builder.get();

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus()
				|| Status.PARTIAL_CONTENT.getStatusCode() == response.getStatus())
			return toBinaryInputStream(response);
		else
			throw handleError(response);
	}

	private String getRangeHeader(Long rangeStart, Long rangeEndInclusive)
	{
		// from given start to end of file
		if (rangeStart != null && rangeStart >= 0 && rangeEndInclusive == null)
		{
			return "bytes=" + rangeStart + "-";
		}
		// from given start to given end (inclusive)
		else if (rangeStart != null && rangeStart >= 0 && rangeEndInclusive != null && rangeEndInclusive > rangeStart)
		{
			return "bytes=" + rangeStart + "-" + rangeEndInclusive;
		}
		// from length + end to end of file
		else if (rangeStart == null && rangeEndInclusive != null && rangeEndInclusive < 0)
		{
			return "bytes=" + rangeEndInclusive;
		}
		else
			return null;
	}

	private BinaryInputStream toBinaryInputStream(Response response)
	{
		long contentLength = getContentLength(response);
		Range range = getRange(response);
		InputStream input = response.readEntity(InputStream.class);

		return new BinaryInputStream(input, contentLength, range);
	}

	private long getContentLength(Response response)
	{
		try
		{
			return Long.parseLong(response.getHeaderString("Content-Length"));
		}
		catch (NumberFormatException e)
		{
			return Long.MIN_VALUE;
		}
	}

	private Range getRange(Response response)
	{
		String contentRange = response.getHeaderString("Content-Range");
		if (contentRange == null)
			return null;

		Matcher matcher = CONTENT_RANGE_PATTERN.matcher(contentRange);
		if (matcher.matches())
		{
			try
			{
				long start = Long.parseLong(matcher.group("start"));
				long end = Long.parseLong(matcher.group("end"));
				long size = Long.parseLong(matcher.group("size"));

				return new Range(size, start, end);
			}
			catch (NumberFormatException e)
			{
			}
		}

		return null;
	}

	@Override
	public Resource read(String resourceTypeName, String id, String version)
	{
		Objects.requireNonNull(resourceTypeName, "resourceTypeName");
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(version, "version");
		if (!RESOURCE_TYPES_BY_NAME.containsKey(resourceTypeName))
			throw new IllegalArgumentException("Resource of type " + resourceTypeName + " not supported");

		Response response = getResource().path(resourceTypeName).path(id).path("_history").path(version).request()
				.accept(Constants.CT_FHIR_JSON_NEW).get();

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return (Resource) response.readEntity(RESOURCE_TYPES_BY_NAME.get(resourceTypeName));
		else
			throw handleError(response);
	}

	@Override
	public <R extends Resource> R read(Class<R> resourceType, String id, String version)
	{
		Objects.requireNonNull(resourceType, "resourceType");
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(version, "version");

		Response response = getResource().path(resourceType.getAnnotation(ResourceDef.class).name()).path(id)
				.path("_history").path(version).request().accept(Constants.CT_FHIR_JSON_NEW).get();

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return response.readEntity(resourceType);
		else
			throw handleError(response);
	}

	@Override
	public <R extends Resource> boolean exists(Class<R> resourceType, String id, String version)
	{
		Objects.requireNonNull(resourceType, "resourceType");
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(version, "version");

		Response response = getResource().path(resourceType.getAnnotation(ResourceDef.class).name()).path(id)
				.path("_history").path(version).request().accept(Constants.CT_FHIR_JSON_NEW).head();

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return true;
		else if (Status.NOT_FOUND.getStatusCode() == response.getStatus())
			return false;
		else
			throw handleError(response);
	}

	@Override
	public BinaryInputStream readBinary(String id, String version, MediaType mediaType)
	{
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(mediaType, "mediaType");

		Response response = getResource().path("Binary").path(id).path("_history").path(version).request()
				.accept(mediaType).get();

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return toBinaryInputStream(response);
		else
			throw handleError(response);
	}

	@Override
	public BinaryInputStream readBinary(String id, String version, MediaType mediaType, Long rangeStart,
			Long rangeEndInclusive, Map<String, String> additionalHeaders)
	{
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(mediaType, "mediaType");

		Builder builder = getResource().path("Binary").path(id).path("_history").path(version).request()
				.accept(mediaType);

		String range = getRangeHeader(rangeStart, rangeEndInclusive);
		if (range != null)
			builder = builder.header("Range", range);

		if (additionalHeaders != null)
		{
			for (Entry<String, String> e : additionalHeaders.entrySet())
				builder = builder.header(e.getKey(), e.getValue());
		}

		Response response = builder.get();

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus()
				|| Status.PARTIAL_CONTENT.getStatusCode() == response.getStatus())
			return toBinaryInputStream(response);
		else
			throw handleError(response);
	}

	@Override
	public boolean exists(IdType resourceTypeIdVersion)
	{
		Objects.requireNonNull(resourceTypeIdVersion, "resourceTypeIdVersion");
		Objects.requireNonNull(resourceTypeIdVersion.getResourceType(), "resourceTypeIdVersion.resourceType");
		Objects.requireNonNull(resourceTypeIdVersion.getIdPart(), "resourceTypeIdVersion.idPart");
		// version may be null

		WebTarget path = getResource().path(resourceTypeIdVersion.getResourceType())
				.path(resourceTypeIdVersion.getIdPart());

		if (resourceTypeIdVersion.hasVersionIdPart())
			path = path.path("_history").path(resourceTypeIdVersion.getVersionIdPart());

		Response response = path.request().accept(Constants.CT_FHIR_JSON_NEW).head();

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return true;
		else if (Status.NOT_FOUND.getStatusCode() == response.getStatus())
			return false;
		else
			throw handleError(response);
	}

	@Override
	public Bundle search(Class<? extends Resource> resourceType, Map<String, List<String>> parameters)
	{
		Objects.requireNonNull(resourceType, "resourceType");

		WebTarget target = getResource().path(resourceType.getAnnotation(ResourceDef.class).name());
		if (parameters != null)
		{
			for (Entry<String, List<String>> entry : parameters.entrySet())
				target = target.queryParam(entry.getKey(), entry.getValue().toArray());
		}

		Response response = target.request().accept(Constants.CT_FHIR_JSON_NEW).get();

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return response.readEntity(Bundle.class);
		else
			throw handleError(response);
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
	public CompletableFuture<Bundle> searchAsync(DelayStrategy delayStrategy, Class<? extends Resource> resourceType,
			Map<String, List<String>> parameters)
	{
		Objects.requireNonNull(resourceType, "resourceType");

		WebTarget target = getResource().path(resourceType.getAnnotation(ResourceDef.class).name());
		if (parameters != null)
		{
			for (Entry<String, List<String>> entry : parameters.entrySet())
				target = target.queryParam(entry.getKey(), entry.getValue().toArray());
		}

		return doSearchAsync(delayStrategy, target, false);
	}

	@Override
	public CompletableFuture<Bundle> searchAsync(DelayStrategy delayStrategy, String url)
	{
		checkUri(url);

		return doSearchAsync(delayStrategy, client.target(url), false);
	}

	@Override
	public CompletableFuture<Bundle> searchAsyncWithStrictHandling(DelayStrategy delayStrategy,
			Class<? extends Resource> resourceType, Map<String, List<String>> parameters)
	{
		Objects.requireNonNull(resourceType, "resourceType");

		WebTarget target = getResource().path(resourceType.getAnnotation(ResourceDef.class).name());
		if (parameters != null)
		{
			for (Entry<String, List<String>> entry : parameters.entrySet())
				target = target.queryParam(entry.getKey(), entry.getValue().toArray());
		}

		return doSearchAsync(delayStrategy, target, true);
	}

	@Override
	public CompletableFuture<Bundle> searchAsyncWithStrictHandling(DelayStrategy delayStrategy, String url)
	{
		checkUri(url);

		return doSearchAsync(delayStrategy, client.target(url), true);
	}

	private void checkUri(String url)
	{
		Objects.requireNonNull(url, "url");
		if (url.isBlank())
			throw new RuntimeException("url is blank");
		if (!url.startsWith(baseUrl))
			throw new RuntimeException("url not starting with client base url");
		if (url.startsWith(baseUrl + "@"))
			throw new RuntimeException("url starting with client base url + @");
	}

	private CompletableFuture<Bundle> doSearchAsync(DelayStrategy delayStrategy, WebTarget target, boolean strict)
	{
		Builder requestBuilder = target.request().header(Constants.HEADER_PREFER,
				Constants.HEADER_PREFER_RESPOND_ASYNC);

		if (strict)
			requestBuilder.header(Constants.HEADER_PREFER, PreferHandlingType.STRICT.getHeaderValue());

		return executeAsync(delayStrategy, requestBuilder, Bundle.class, PreferReturnType.REPRESENTATION,
				AsyncInvoker::get).thenApply(PreferReturn::resource);
	}

	private <R extends Resource> CompletableFuture<PreferReturn<R>> executeAsync(DelayStrategy delayStrategy,
			Builder requestBuilder, Class<R> returnType, PreferReturnType preferReturnType,
			BiFunction<AsyncInvoker, InvocationCallback<Response>, Future<Response>> httpMethod)
	{
		CompletableFuture<PreferReturn<R>> resultFuture = new CompletableFuture<>();
		InvocationCallback<Response> callback = new InvocationCallback<Response>()
		{
			@Override
			public void completed(Response response)
			{
				if (Status.OK.getStatusCode() == response.getStatus())
				{
					PreferReturn<R> preferReturn = toPreferReturn(preferReturnType, returnType, response);
					resultFuture.complete(preferReturn);
				}
				else if (Status.ACCEPTED.getStatusCode() == response.getStatus())
				{
					response.close();

					Optional<Duration> retryAfter = parseRetryAfter(response.getHeaderString(HttpHeaders.RETRY_AFTER));

					String contentLocation = response.getHeaderString(HttpHeaders.CONTENT_LOCATION);
					if (contentLocation != null && !contentLocation.isBlank())
					{
						checkUri(contentLocation);
						pollUntilComplete(contentLocation, false, delayStrategy, retryAfter, resultFuture, returnType,
								preferReturnType);

						return;
					}

					String location = response.getHeaderString(HttpHeaders.LOCATION);
					if (location != null && !location.isBlank())
					{
						checkUri(location);
						pollUntilComplete(location, true, delayStrategy, retryAfter, resultFuture, returnType,
								preferReturnType);

						return;
					}

					resultFuture.completeExceptionally(
							new WebApplicationException("Reponse from server without " + HttpHeaders.CONTENT_LOCATION
									+ " or " + HttpHeaders.LOCATION + " header", Status.BAD_GATEWAY));
				}
				else
					resultFuture.completeExceptionally(handleError(response));
			}

			@Override
			public void failed(Throwable throwable)
			{
				resultFuture.completeExceptionally(throwable);
			}
		};

		AsyncInvoker async = requestBuilder.header(Constants.HEADER_PREFER, preferReturnType.getHeaderValue())
				.accept(Constants.CT_FHIR_JSON_NEW).async();

		httpMethod.apply(async, callback);

		return resultFuture;
	}

	private <R extends Resource> void pollUntilComplete(String location, boolean sendAcceptHeader,
			DelayStrategy delayStrategy, Optional<Duration> retryAfter, CompletableFuture<PreferReturn<R>> resultFuture,
			Class<R> resourceType, PreferReturnType preferReturnType)
	{
		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

		Runnable poll = new Runnable()
		{
			private Duration delay = delayStrategy.getFirstDelay();

			@Override
			public void run()
			{
				if (resultFuture.isCancelled())
				{
					executor.shutdownNow();
					return;
				}

				try
				{
					Builder request = client.target(location).request();
					if (sendAcceptHeader)
						request = request.accept(Constants.CT_FHIR_JSON_NEW);

					Response response = request.get();

					if (Status.OK.getStatusCode() == response.getStatus())
					{
						Bundle bundle = response.readEntity(Bundle.class);
						PreferReturn<R> r = DsfClientJersey.this.unpack(bundle, resourceType, preferReturnType);

						resultFuture.complete(r);
						executor.shutdownNow();
					}
					else if (Status.ACCEPTED.getStatusCode() == response.getStatus())
					{
						response.close();

						Optional<Duration> retryAfter = parseRetryAfter(
								response.getHeaderString(HttpHeaders.RETRY_AFTER));

						delay = retryAfter.orElse(delayStrategy.getNextDelay(delay));
						logger.debug("Status 202, trying again in {}", delay);
						executor.schedule(this, delay.toMillis(), TimeUnit.MILLISECONDS);
					}
					else
					{
						resultFuture.completeExceptionally(handleError(response));
						executor.shutdownNow();
					}
				}
				catch (Exception e)
				{
					resultFuture.completeExceptionally(e);
					executor.shutdownNow();
				}
			}
		};

		Duration delay = retryAfter.orElse(delayStrategy.getFirstDelay());
		logger.debug("Status 202, trying again in {}", delay);
		executor.schedule(poll, delay.toMillis(), TimeUnit.MILLISECONDS);
	}

	private Optional<Duration> parseRetryAfter(String headerValue)
	{
		if (headerValue == null || headerValue.isBlank())
			return Optional.empty();

		String trimmed = headerValue.trim();
		if (trimmed.chars().allMatch(Character::isDigit))
		{
			try
			{
				long seconds = Long.parseLong(trimmed);
				return Optional.of(Duration.ofSeconds(seconds));
			}
			catch (NumberFormatException e)
			{
				logger.warn("Unable to parse header value: {}", e.getMessage());
				return Optional.empty();
			}
		}

		try
		{
			ZonedDateTime retryTime = ZonedDateTime.parse(trimmed, DateTimeFormatter.ofPattern(RFC_7231_FORMAT));
			Duration duration = Duration.between(ZonedDateTime.now(), retryTime);
			return Optional.of(duration.isNegative() ? Duration.ZERO : duration);
		}
		catch (DateTimeParseException e)
		{
			logger.warn("Unable to parse header value: {}", e.getMessage());
			return Optional.empty();
		}
	}

	private <R extends Resource> PreferReturn<R> unpack(Bundle bundle, Class<R> resourceType,
			PreferReturnType preferReturnType)
	{
		if (BundleType.BATCHRESPONSE.equals(bundle.getType()))
		{
			List<BundleEntryComponent> entries = bundle.getEntry();
			if (entries.size() == 1)
			{
				BundleEntryComponent entry = entries.get(0);
				if (entry.hasResponse())
				{
					BundleEntryResponseComponent response = entry.getResponse();
					if (response.hasStatus())
					{
						String status = response.getStatus();
						if ("200 OK".equals(status) || "200".equals(status))
						{
							if (PreferReturnType.MINIMAL.equals(preferReturnType))
								return PreferReturn.minimal(response.getLocation());
							else if (PreferReturnType.OPERATION_OUTCOME.equals(preferReturnType))
							{
								if (response.hasOutcome())
								{
									Resource outcome = response.getOutcome();

									if (outcome instanceof OperationOutcome o)
										return PreferReturn.outcome(o);
									else
										throw new ProcessingException(
												"Reponse from server not a Bundle with Bundle.entry[0].response.outcome of type OperationOutcome but "
														+ outcome.getResourceType().name());
								}
								else
									throw new ProcessingException(
											"Reponse from server not a Bundle with Bundle.entry[0].response.outcome");
							}
							else if (PreferReturnType.REPRESENTATION.equals(preferReturnType))
							{
								if (entry.hasResource())
								{
									Resource resource = entry.getResource();

									if (resourceType.isInstance(resource))
										return PreferReturn.resource(resourceType.cast(resource));
									else
									{
										String resourceTypeName = resourceType.getAnnotation(ResourceDef.class).name();

										throw new ProcessingException(
												"Reponse from server not a Bundle with Bundle.entry[0].resource of type "
														+ resourceTypeName + " but "
														+ resource.getResourceType().name());
									}
								}
								else
									throw new ProcessingException(
											"Reponse from server not a Bundle with Bundle.entry[0].resource");
							}
							else
								throw new IllegalArgumentException(preferReturnType + " not supported");
						}
						else
						{
							Matcher statusMatcher = HTTP_STATUS_PATTERN.matcher(status);
							if (statusMatcher.matches())
							{
								try
								{
									int code = Integer.parseInt(statusMatcher.group());
									throw new WebApplicationException("Bundle.entry[0].response.status: " + status,
											Status.fromStatusCode(code));
								}
								catch (NumberFormatException e)
								{
									throw new ProcessingException(
											"Reponse from server not a Bundle with unkown Bundle.entry[0].response.status: "
													+ status,
											e);
								}
							}
							else
								throw new ProcessingException(
										"Reponse from server not a Bundle with unkown Bundle.entry[0].response.status: "
												+ status);
						}
					}
					else
						throw new ProcessingException(
								"Reponse from server not a Bundle with Bundle.entry[0].response.status");
				}
				else
					throw new ProcessingException("Reponse from server not a Bundle with Bundle.entry[0].response");
			}
			else
				throw new ProcessingException("Reponse from server not a Bundle with one entry but " + entries.size());
		}
		else
			throw new ProcessingException("Reponse from server not a Bundle with type " + BundleType.BATCHRESPONSE
					+ " but " + bundle.getType());
	}

	@Override
	public CapabilityStatement getConformance()
	{
		Response response = getResource().path("metadata").request()
				.accept(Constants.CT_FHIR_JSON_NEW + "; fhirVersion=4.0").get();

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());

		if (Status.OK.getStatusCode() == response.getStatus())
			return response.readEntity(CapabilityStatement.class);
		else
			throw handleError(response);
	}

	@Override
	public StructureDefinition generateSnapshot(String url)
	{
		Objects.requireNonNull(url, "url");

		Parameters parameters = new Parameters();
		parameters.addParameter().setName("url").setValue(new UriType(url));

		Response response = getResource().path(StructureDefinition.class.getAnnotation(ResourceDef.class).name())
				.path("$snapshot").request().accept(Constants.CT_FHIR_JSON_NEW)
				.post(Entity.entity(parameters, Constants.CT_FHIR_JSON_NEW));

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return response.readEntity(StructureDefinition.class);
		else
			throw handleError(response);
	}

	@Override
	public StructureDefinition generateSnapshot(StructureDefinition differential)
	{
		Objects.requireNonNull(differential, "differential");

		Parameters parameters = new Parameters();
		parameters.addParameter().setName("resource").setResource(differential);

		Response response = getResource().path(StructureDefinition.class.getAnnotation(ResourceDef.class).name())
				.path("$snapshot").request().accept(Constants.CT_FHIR_JSON_NEW)
				.post(Entity.entity(parameters, Constants.CT_FHIR_JSON_NEW));

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return response.readEntity(StructureDefinition.class);
		else
			throw handleError(response);
	}

	@Override
	public BasicDsfClient withRetry(int nTimes, DelayStrategy delayStrategy)
	{
		if (nTimes < 0)
			throw new IllegalArgumentException("nTimes < 0");
		Objects.requireNonNull(delayStrategy, "delayStrategy");

		return new BasicDsfClientWithRetryImpl(this, nTimes, delayStrategy);
	}

	@Override
	public BasicDsfClient withRetryForever(DelayStrategy delayStrategy)
	{
		Objects.requireNonNull(delayStrategy, "delayStrategy");

		return new BasicDsfClientWithRetryImpl(this, RETRY_FOREVER, delayStrategy);
	}

	@Override
	public Bundle history(Class<? extends Resource> resourceType, String id, int page, int count)
	{
		WebTarget target = getResource();

		if (resourceType != null)
			target = target.path(resourceType.getAnnotation(ResourceDef.class).name());

		if (!StringUtils.isBlank(id))
			target = target.path(id);

		if (page != Integer.MIN_VALUE)
			target = target.queryParam("_page", page);

		if (count != Integer.MIN_VALUE)
			target = target.queryParam("_count", count);

		Response response = target.path("_history").request().accept(Constants.CT_FHIR_JSON_NEW).get();

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return response.readEntity(Bundle.class);
		else
			throw handleError(response);
	}

	<R extends Resource> PreferReturn<R> operation(PreferReturnType preferReturnType, String operationName,
			Parameters parameters, Class<R> returnType)
	{
		Objects.requireNonNull(preferReturnType, "preferReturnType");
		Objects.requireNonNull(operationName, "operationName");
		// parameters may be null
		Objects.requireNonNull(returnType, "returnType");

		operationName = !operationName.startsWith("$") ? "$" + operationName : operationName;

		Response response = getResource().path(operationName).request()
				.header(Constants.HEADER_PREFER, preferReturnType.getHeaderValue()).accept(Constants.CT_FHIR_JSON_NEW)
				.post(Entity.entity(parameters, Constants.CT_FHIR_JSON_NEW));

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return toPreferReturn(preferReturnType, returnType, response);
		else
			throw handleError(response);
	}

	<R extends Resource, T extends Resource> PreferReturn<R> operation(PreferReturnType preferReturnType,
			Class<T> resourceType, String operationName, Parameters parameters, Class<R> returnType)
	{
		Objects.requireNonNull(preferReturnType, "preferReturnType");
		Objects.requireNonNull(resourceType, "resourceType");
		Objects.requireNonNull(operationName, "operationName");
		// parameters may be null
		Objects.requireNonNull(returnType, "returnType");

		operationName = !operationName.startsWith("$") ? "$" + operationName : operationName;

		Response response = getResource().path(resourceType.getAnnotation(ResourceDef.class).name()).path(operationName)
				.request().header(Constants.HEADER_PREFER, preferReturnType.getHeaderValue())
				.accept(Constants.CT_FHIR_JSON_NEW).post(Entity.entity(parameters, Constants.CT_FHIR_JSON_NEW));

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return toPreferReturn(preferReturnType, returnType, response);
		else
			throw handleError(response);
	}

	<R extends Resource, T extends Resource> PreferReturn<R> operation(PreferReturnType preferReturnType,
			Class<T> resourceType, String id, String operationName, Parameters parameters, Class<R> returnType)
	{
		Objects.requireNonNull(preferReturnType, "preferReturnType");
		Objects.requireNonNull(resourceType, "resourceType");
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(operationName, "operationName");
		// parameters may be null
		Objects.requireNonNull(returnType, "returnType");

		operationName = !operationName.startsWith("$") ? "$" + operationName : operationName;

		Response response = getResource().path(resourceType.getAnnotation(ResourceDef.class).name()).path(id)
				.path(operationName).request().header(Constants.HEADER_PREFER, preferReturnType.getHeaderValue())
				.accept(Constants.CT_FHIR_JSON_NEW).post(Entity.entity(parameters, Constants.CT_FHIR_JSON_NEW));

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return toPreferReturn(preferReturnType, returnType, response);
		else
			throw handleError(response);
	}

	<R extends Resource, T extends Resource> PreferReturn<R> operation(PreferReturnType preferReturnType,
			Class<T> resourceType, String id, String version, String operationName, Parameters parameters,
			Class<R> returnType)
	{
		Objects.requireNonNull(preferReturnType, "preferReturnType");
		Objects.requireNonNull(resourceType, "resourceType");
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(operationName, "operationName");
		// parameters may be null
		Objects.requireNonNull(returnType, "returnType");

		operationName = !operationName.startsWith("$") ? "$" + operationName : operationName;

		Response response = getResource().path(resourceType.getAnnotation(ResourceDef.class).name()).path(id)
				.path("_history").path(version).path(operationName).request()
				.header(Constants.HEADER_PREFER, preferReturnType.getHeaderValue()).accept(Constants.CT_FHIR_JSON_NEW)
				.post(Entity.entity(parameters, Constants.CT_FHIR_JSON_NEW));

		logger.debug("HTTP {}: {}", response.getStatusInfo().getStatusCode(),
				response.getStatusInfo().getReasonPhrase());
		if (Status.OK.getStatusCode() == response.getStatus())
			return toPreferReturn(preferReturnType, returnType, response);
		else
			throw handleError(response);
	}

	@Override
	public <R extends Resource> R operation(String operationName, Parameters parameters, Class<R> returnType)
	{
		return operation(PreferReturnType.REPRESENTATION, operationName, parameters, returnType).resource();
	}

	@Override
	public <R extends Resource, T extends Resource> R operation(Class<T> resourceType, String operationName,
			Parameters parameters, Class<R> returnType)
	{
		return operation(PreferReturnType.REPRESENTATION, resourceType, operationName, parameters, returnType)
				.resource();
	}

	@Override
	public <R extends Resource, T extends Resource> R operation(Class<T> resourceType, String id, String operationName,
			Parameters parameters, Class<R> returnType)
	{
		return operation(PreferReturnType.REPRESENTATION, resourceType, id, operationName, parameters, returnType)
				.resource();
	}

	@Override
	public <R extends Resource, T extends Resource> R operation(Class<T> resourceType, String id, String version,
			String operationName, Parameters parameters, Class<R> returnType)
	{
		return operation(PreferReturnType.REPRESENTATION, resourceType, id, version, operationName, parameters,
				returnType).resource();
	}

	<R extends Resource> CompletableFuture<PreferReturn<R>> operationAsync(PreferReturnType preferReturnType,
			DelayStrategy delayStrategy, String operationName, Parameters parameters, Class<R> returnType)
	{
		Objects.requireNonNull(preferReturnType, "preferReturnType");
		Objects.requireNonNull(delayStrategy, "delayStrategy");
		Objects.requireNonNull(operationName, "operationName");
		// parameters may be null
		Objects.requireNonNull(returnType, "returnType");

		operationName = !operationName.startsWith("$") ? "$" + operationName : operationName;

		Builder requestBuilder = getResource().path(operationName).request()
				.header(Constants.HEADER_PREFER, preferReturnType.getHeaderValue()).accept(Constants.CT_FHIR_JSON_NEW);

		return executeAsync(delayStrategy, requestBuilder, returnType, preferReturnType,
				(async, callback) -> async.post(Entity.entity(parameters, Constants.CT_FHIR_JSON_NEW), callback));
	}

	<R extends Resource, T extends Resource> CompletableFuture<PreferReturn<R>> operationAsync(
			PreferReturnType preferReturnType, DelayStrategy delayStrategy, Class<T> resourceType, String operationName,
			Parameters parameters, Class<R> returnType)
	{
		Objects.requireNonNull(preferReturnType, "preferReturnType");
		Objects.requireNonNull(delayStrategy, "delayStrategy");
		Objects.requireNonNull(resourceType, "resourceType");
		Objects.requireNonNull(operationName, "operationName");
		// parameters may be null
		Objects.requireNonNull(returnType, "returnType");

		operationName = !operationName.startsWith("$") ? "$" + operationName : operationName;

		Builder requestBuilder = getResource().path(resourceType.getAnnotation(ResourceDef.class).name())
				.path(operationName).request().header(Constants.HEADER_PREFER, preferReturnType.getHeaderValue())
				.accept(Constants.CT_FHIR_JSON_NEW);

		return executeAsync(delayStrategy, requestBuilder, returnType, preferReturnType,
				(async, callback) -> async.post(Entity.entity(parameters, Constants.CT_FHIR_JSON_NEW), callback));
	}

	<R extends Resource, T extends Resource> CompletableFuture<PreferReturn<R>> operationAsync(
			PreferReturnType preferReturnType, DelayStrategy delayStrategy, Class<T> resourceType, String id,
			String operationName, Parameters parameters, Class<R> returnType)
	{
		Objects.requireNonNull(preferReturnType, "preferReturnType");
		Objects.requireNonNull(delayStrategy, "delayStrategy");
		Objects.requireNonNull(resourceType, "resourceType");
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(operationName, "operationName");
		// parameters may be null
		Objects.requireNonNull(returnType, "returnType");

		operationName = !operationName.startsWith("$") ? "$" + operationName : operationName;

		Builder requestBuilder = getResource().path(resourceType.getAnnotation(ResourceDef.class).name()).path(id)
				.path(operationName).request().header(Constants.HEADER_PREFER, preferReturnType.getHeaderValue())
				.accept(Constants.CT_FHIR_JSON_NEW);

		return executeAsync(delayStrategy, requestBuilder, returnType, preferReturnType,
				(async, callback) -> async.post(Entity.entity(parameters, Constants.CT_FHIR_JSON_NEW), callback));
	}

	<R extends Resource, T extends Resource> CompletableFuture<PreferReturn<R>> operationAsync(
			PreferReturnType preferReturnType, DelayStrategy delayStrategy, Class<T> resourceType, String id,
			String version, String operationName, Parameters parameters, Class<R> returnType)
	{
		Objects.requireNonNull(preferReturnType, "preferReturnType");
		Objects.requireNonNull(delayStrategy, "delayStrategy");
		Objects.requireNonNull(resourceType, "resourceType");
		Objects.requireNonNull(id, "id");
		Objects.requireNonNull(version, "version");
		Objects.requireNonNull(operationName, "operationName");
		// parameters may be null
		Objects.requireNonNull(returnType, "returnType");

		operationName = !operationName.startsWith("$") ? "$" + operationName : operationName;

		Builder requestBuilder = getResource().path(resourceType.getAnnotation(ResourceDef.class).name()).path(id)
				.path("_history").path(version).path(operationName).request()
				.header(Constants.HEADER_PREFER, preferReturnType.getHeaderValue()).accept(Constants.CT_FHIR_JSON_NEW);

		return executeAsync(delayStrategy, requestBuilder, returnType, preferReturnType,
				(async, callback) -> async.post(Entity.entity(parameters, Constants.CT_FHIR_JSON_NEW), callback));
	}

	@Override
	public <R extends Resource> CompletableFuture<R> operationAsync(DelayStrategy delayStrategy, String operationName,
			Parameters parameters, Class<R> returnType)
	{
		return operationAsync(PreferReturnType.REPRESENTATION, delayStrategy, operationName, parameters, returnType)
				.thenApply(PreferReturn::resource);
	}

	@Override
	public <R extends Resource, T extends Resource> CompletableFuture<R> operationAsync(DelayStrategy delayStrategy,
			Class<T> resourceType, String operationName, Parameters parameters, Class<R> returnType)
	{
		return operationAsync(PreferReturnType.REPRESENTATION, delayStrategy, resourceType, operationName, parameters,
				returnType).thenApply(PreferReturn::resource);
	}

	@Override
	public <R extends Resource, T extends Resource> CompletableFuture<R> operationAsync(DelayStrategy delayStrategy,
			Class<T> resourceType, String id, String operationName, Parameters parameters, Class<R> returnType)
	{
		return operationAsync(PreferReturnType.REPRESENTATION, delayStrategy, resourceType, id, operationName,
				parameters, returnType).thenApply(PreferReturn::resource);
	}

	@Override
	public <R extends Resource, T extends Resource> CompletableFuture<R> operationAsync(DelayStrategy delayStrategy,
			Class<T> resourceType, String id, String version, String operationName, Parameters parameters,
			Class<R> returnType)
	{
		return operationAsync(PreferReturnType.REPRESENTATION, delayStrategy, resourceType, id, version, operationName,
				parameters, returnType).thenApply(PreferReturn::resource);
	}
}
