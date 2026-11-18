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
package dev.dsf.bpe.subscription;

import java.net.SocketException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.WebSocketHandshakeException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import org.eclipse.jetty.websocket.core.exception.UpgradeException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.parser.IParser;
import dev.dsf.bpe.client.dsf.ClientProvider;
import dev.dsf.bpe.client.dsf.WebserviceClient;
import dev.dsf.fhir.client.Endpoint;
import dev.dsf.fhir.client.WebsocketClient;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response.Status;

public class FhirSubscriptionConnectorImpl<R extends Resource> implements Endpoint, FhirSubscriptionConnector
{
	private static final Logger logger = LoggerFactory.getLogger(FhirSubscriptionConnectorImpl.class);

	private final FhirContext fhirContext;
	private final Class<R> resourceType;
	private final String resourceTypeName;
	private final Executor executor;
	private final SubscriptionHandlerFactory<R> subscriptionHandlerFactory;
	private final Map<String, List<String>> subscriptionSearchParameter;
	private final WebsocketClient websocketClient;
	private final WebserviceClient webserviceClient;

	private volatile Consumer<String> handler;

	public FhirSubscriptionConnectorImpl(FhirContext fhirContext, Class<R> resourceType, Executor executor,
			ClientProvider clientProvider, SubscriptionHandlerFactory<R> subscriptionHandlerFactory,
			String subscriptionSearchParameter)
	{
		this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext");
		this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
		this.resourceTypeName = resourceType.getAnnotation(ResourceDef.class).name();
		this.executor = Objects.requireNonNull(executor, "executor");
		this.subscriptionHandlerFactory = Objects.requireNonNull(subscriptionHandlerFactory,
				"subscriptionHandlerFactory");
		this.subscriptionSearchParameter = parseSubscriptionSearchParameter(subscriptionSearchParameter);

		websocketClient = clientProvider.createWebsocketClient(this, toKebabCase(resourceTypeName));
		webserviceClient = clientProvider.getWebserviceClient();
	}

	private Map<String, List<String>> parseSubscriptionSearchParameter(String queryParameters)
	{
		Objects.requireNonNull(queryParameters, "queryParameters");

		return UriComponentsBuilder
				.fromUriString(queryParameters.startsWith("?") ? queryParameters : "?" + queryParameters).build()
				.getQueryParams();
	}

	@Override
	public void connect()
	{
		websocketClient.connect();
	}

	private static String toKebabCase(String input)
	{
		return input.replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase(Locale.ENGLISH);
	}

	@Override
	public boolean isConnected()
	{
		return websocketClient.isConnected();
	}

	@Override
	public List<Long> getWebsocketLatency()
	{
		return websocketClient.getPingHistory();
	}

	@Override
	public void close() throws Exception
	{
		websocketClient.close();
	}

	@Override
	public Subscription getSubscription()
	{
		logger.debug("Retrieving {} websocket subscription ...", resourceTypeName);

		Bundle bundle = webserviceClient.searchWithStrictHandling(Subscription.class, subscriptionSearchParameter);

		if (!Bundle.BundleType.SEARCHSET.equals(bundle.getType()))
			throw new RuntimeException("Could not retrieve searchset for subscription search query "
					+ subscriptionSearchParameter + ", but got " + bundle.getType());
		if (bundle.getTotal() != 1)
			throw new RuntimeException("Could not retrieve exactly one result for subscription search query "
					+ subscriptionSearchParameter);
		if (!(bundle.getEntryFirstRep().getResource() instanceof Subscription))
			throw new RuntimeException("Could not retrieve exactly one Subscription for subscription search query "
					+ subscriptionSearchParameter + ", but got "
					+ bundle.getEntryFirstRep().getResource().getResourceType());

		Subscription subscription = (Subscription) bundle.getEntryFirstRep().getResource();

		logger.debug("{} subscription with id {} found", resourceTypeName, subscription.getIdElement().getIdPart());

		return subscription;
	}

	@Override
	public void beforeConnect(Subscription subscription)
	{
		ExistingResourceLoader<R> existingResourceLoader = subscriptionHandlerFactory.createExistingResourceLoader(
				webserviceClient, parseSubscriptionCriteria(subscription.getCriteria(), resourceTypeName));

		try
		{
			logger.debug("Reading existing {} resources", resourceTypeName);
			existingResourceLoader.readExistingResources();
		}
		catch (Exception e)
		{
			logger.debug("Unable to read existing {} resources: {} - {}", resourceTypeName, e.getClass().getName(),
					e.getMessage());
			throw e;
		}

		EventType eventType = EventType.fromSubscription(subscription);
		if (EventType.PING.equals(eventType))
		{
			PingEventResourceHandler<R> pingEventResourceHandler = subscriptionHandlerFactory
					.createPingEventResourceHandler(existingResourceLoader);
			handler = message -> pingEventResourceHandler.onPing(message, subscription.getIdElement().getIdPart());
		}
		else
		{
			EventResourceHandler<R> eventResourceHandler = subscriptionHandlerFactory.createEventResourceHandler();
			handler = message -> eventResourceHandler
					.onResource(createParser(eventType).parseResource(resourceType, message));
		}
	}

	private Map<String, List<String>> parseSubscriptionCriteria(String queryParameters, String expectedPath)
	{
		Objects.requireNonNull(queryParameters, "queryParameters");

		if (expectedPath != null && !expectedPath.isBlank())
		{
			UriComponents components = UriComponentsBuilder.fromUriString(queryParameters).build();
			if (!expectedPath.equals(components.getPath()))
				throw new RuntimeException("Unexpected query parameters format '" + queryParameters + "'");
			else
				return components.getQueryParams();
		}
		else
		{
			UriComponents components = UriComponentsBuilder
					.fromUriString(queryParameters.startsWith("?") ? queryParameters : "?" + queryParameters).build();

			return components.getQueryParams();
		}
	}

	@Override
	public CompletableFuture<Void> onMessage(Subscription subscription, String message)
	{
		Objects.requireNonNull(subscription, "subscription");
		Objects.requireNonNull(message, "message");

		if (handler == null)
			throw new IllegalStateException("No handler set");

		return CompletableFuture.supplyAsync(() ->
		{
			handler.accept(message);
			return null;
		}, executor);
	}

	private IParser createParser(EventType eventType)
	{
		return switch (eventType)
		{
			case XML -> configureParser(fhirContext.newXmlParser());
			case JSON -> configureParser(fhirContext.newJsonParser());

			default -> throw new RuntimeException("EventType " + eventType + " not supported");
		};
	}

	private IParser configureParser(IParser p)
	{
		p.setStripVersionsFromReferences(false);
		p.setOverrideResourceIdWithBundleEntryFullUrl(false);
		return p;
	}

	@Override
	public void onOpen()
	{
		logger.info("{} websocket connected", resourceTypeName);
	}

	@Override
	public void onDisconnect()
	{
		logger.info("{} websocket disconnected", resourceTypeName);
		handler = null;
	}

	@Override
	public boolean isRetryableException(Throwable e)
	{
		Throwable u = unwrapException(e);

		if (u instanceof WebApplicationException w
				&& Status.Family.SERVER_ERROR.equals(w.getResponse().getStatusInfo().getFamily()))
			return true;
		if (u instanceof HttpConnectTimeoutException)
			return true;
		if (u instanceof SocketException)
			return true;
		if (u instanceof WebSocketHandshakeException)
			return true;
		// TODO outside a docker environment this usually is not a transient error, remove ?
		if (u instanceof UnknownHostException)
			return true;

		return false;
	}

	private Throwable unwrapException(Throwable e)
	{
		while ((e instanceof ProcessingException || e instanceof CompletionException || e instanceof ExecutionException
				|| e instanceof UpgradeException) && e.getCause() != null)
			e = e.getCause();

		return e;
	}
}
