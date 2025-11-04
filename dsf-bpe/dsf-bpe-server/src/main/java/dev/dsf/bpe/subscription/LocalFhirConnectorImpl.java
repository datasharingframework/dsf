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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.ResourceDef;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.Constants;
import dev.dsf.bpe.client.dsf.ClientProvider;
import dev.dsf.bpe.client.dsf.WebserviceClient;
import dev.dsf.fhir.client.WebsocketClient;

public class LocalFhirConnectorImpl<R extends Resource> implements LocalFhirConnector, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(LocalFhirConnectorImpl.class);

	private final Class<R> resourceType;
	private final String resourceName;
	private final ClientProvider clientProvider;
	private final FhirContext fhirContext;
	private final SubscriptionHandlerFactory<R> subscriptionHandlerFactory;
	private final Duration retrySleep;
	private final int maxRetries;
	private final Map<String, List<String>> subscriptionSearchParameter;

	public LocalFhirConnectorImpl(Class<R> resourceType, ClientProvider clientProvider,
			SubscriptionHandlerFactory<R> subscriptionHandlerFactory, FhirContext fhirContext,
			String subscriptionSearchParameter, Duration retrySleep, int maxRetries)
	{
		this.resourceType = resourceType;
		this.resourceName = resourceType == null ? null : resourceType.getAnnotation(ResourceDef.class).name();
		this.clientProvider = clientProvider;
		this.subscriptionHandlerFactory = subscriptionHandlerFactory;
		this.fhirContext = fhirContext;
		this.subscriptionSearchParameter = parse(subscriptionSearchParameter, null);
		this.retrySleep = retrySleep;
		this.maxRetries = maxRetries;
	}

	private Map<String, List<String>> parse(String queryParameters, String expectedPath)
	{
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
			UriComponents componentes = UriComponentsBuilder
					.fromUriString(queryParameters.startsWith("?") ? queryParameters : "?" + queryParameters).build();

			return componentes.getQueryParams();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(resourceType, "resourceType");
		Objects.requireNonNull(clientProvider, "clientProvider");
		Objects.requireNonNull(subscriptionHandlerFactory, "subscriptionHandlerFactory");
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(subscriptionSearchParameter, "subscriptionSearchParameter");

		if (retrySleep.isNegative())
			throw new IllegalArgumentException("retrySleepMillis negative");

		// maxRetries < 0 => retry forever
	}

	@Override
	public void connect()
	{
		logger.debug("Retrieving Subscription and connecting to websocket");

		CompletableFuture.supplyAsync(this::retrieveWebsocketSubscription, Executors.newSingleThreadExecutor())
				.thenApply(this::loadNewResources).thenAccept(this::connectWebsocket).exceptionally(this::onError);
	}

	private Subscription retrieveWebsocketSubscription()
	{
		try
		{
			if (maxRetries >= 0)
				return retry(this::doRetrieveWebsocketSubscription);
			else
				return retryForever(this::doRetrieveWebsocketSubscription);
		}
		catch (Exception e)
		{
			logger.debug("Error while retrieving {} websocket subscription", resourceName, e);
			logger.warn("Error while retrieving {} websocket subscription: {} - {}", resourceName,
					e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	private Subscription retry(Supplier<Subscription> supplier)
	{
		RuntimeException lastException = null;
		for (int retryCounter = 0; retryCounter <= maxRetries; retryCounter++)
		{
			try
			{
				return supplier.get();
			}
			catch (RuntimeException e)
			{
				if (retryCounter < maxRetries)
				{
					logger.warn(
							"Error while retrieving {} websocket subscription ({}), trying again in {}s (retry {} of {})",
							resourceName, e.getMessage(), retrySleep.toSeconds(), retryCounter + 1, maxRetries);
					try
					{
						Thread.sleep(retrySleep);
					}
					catch (InterruptedException e1)
					{
					}
				}

				lastException = e;
			}
		}

		logger.warn("Error while retrieving {} websocket subscription ({}), giving up", resourceName,
				lastException.getMessage());

		throw lastException;
	}

	private Subscription retryForever(Supplier<Subscription> supplier)
	{
		for (int retryCounter = 1; true; retryCounter++)
		{
			try
			{
				return supplier.get();
			}
			catch (RuntimeException e)
			{
				logger.warn("Error while retrieving {} websocket subscription ({}), trying again in {}s (retry {})",
						resourceName, e.getMessage(), retrySleep.toSeconds(), retryCounter);
				try
				{
					Thread.sleep(retrySleep);
				}
				catch (InterruptedException e1)
				{
				}
			}
		}
	}

	private Subscription doRetrieveWebsocketSubscription()
	{
		logger.debug("Retrieving {} websocket subscription ...", resourceName);

		Bundle bundle = clientProvider.getWebserviceClient().searchWithStrictHandling(Subscription.class,
				subscriptionSearchParameter);

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
		logger.debug("Subscription with id {} found", subscription.getIdElement().getIdPart());

		return subscription;
	}

	private Subscription loadNewResources(Subscription subscription)
	{
		try
		{
			logger.info("Downloading new {} resources ...", resourceName);

			WebserviceClient client = clientProvider.getWebserviceClient();
			ExistingResourceLoader<R> existingResourceLoader = subscriptionHandlerFactory
					.createExistingResourceLoader(client);
			Map<String, List<String>> subscriptionCriteria = parse(subscription.getCriteria(),
					resourceType.getAnnotation(ResourceDef.class).name());
			existingResourceLoader.readExistingResources(subscriptionCriteria);

			logger.info("Downloading new {} resources [Done]", resourceName);

			return subscription;
		}
		catch (Exception e)
		{
			logger.debug("Error while downloading new {} resources", resourceName, e);
			logger.warn("Error while downloading new {} resources: {} - {}", resourceName, e.getClass().getName(),
					e.getMessage());

			throw e;
		}
	}

	private void connectWebsocket(Subscription subscription)
	{
		try
		{
			WebsocketClient client = clientProvider.getWebsocketClient(this::connect,
					subscription.getIdElement().getIdPart());

			EventType eventType = toEventType(subscription.getChannel().getPayload());
			if (EventType.PING.equals(eventType))
			{
				Map<String, List<String>> subscriptionCriteria = parse(subscription.getCriteria(),
						resourceType.getAnnotation(ResourceDef.class).name());
				setPingEventHandler(client, subscription.getIdElement().getIdPart(), subscriptionCriteria);
			}
			else
				setResourceEventHandler(client, eventType);

			logger.info("Connecting {} websocket to local DSF FHIR server, subscription: {} ...", resourceName,
					subscription.getIdElement().getIdPart());

			client.connect();
		}
		catch (Exception e)
		{
			logger.debug("Unable to connect {} websocket to local DSF FHIR server", resourceName, e);
			logger.warn("Unable to connect {} websocket to local DSF FHIR server: {} - {}", resourceName,
					e.getClass().getName(), e.getMessage());

			throw e;
		}
	}

	private Void onError(Throwable t)
	{
		// no debug log, exception previously logged by retrieveWebsocketSubscription, loadNewResources and
		// connectWebsocket methods
		logger.error("Error loading existing {} resources and connecting websocket: {} - {}", resourceName,
				t.getClass().getName(), t.getMessage());

		return null;
	}

	private EventType toEventType(String payload)
	{
		if (payload == null)
			return EventType.PING;

		return switch (payload)
		{
			case Constants.CT_FHIR_JSON, Constants.CT_FHIR_JSON_NEW -> EventType.JSON;
			case Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW -> EventType.XML;

			default -> throw new RuntimeException("Unsupportet subscription.payload " + payload);
		};
	}

	@EventListener({ ContextClosedEvent.class })
	public void onContextClosedEvent()
	{
		clientProvider.disconnectAll();
	}

	private void setPingEventHandler(WebsocketClient client, String subscriptionIdPart,
			Map<String, List<String>> searchCriteriaQueryParameters)
	{
		WebserviceClient webserviceClient = clientProvider.getWebserviceClient();
		ExistingResourceLoader<R> existingResourceLoader = subscriptionHandlerFactory
				.createExistingResourceLoader(webserviceClient);
		PingEventResourceHandler<R> pingHandler = subscriptionHandlerFactory
				.createPingEventResourceHandler(existingResourceLoader);
		client.setPingHandler(ping -> pingHandler.onPing(ping, subscriptionIdPart, searchCriteriaQueryParameters));
	}

	private void setResourceEventHandler(WebsocketClient client, EventType eventType)
	{
		EventResourceHandler<R> eventHandler = subscriptionHandlerFactory.createEventResourceHandler();
		client.setResourceHandler(r -> eventHandler.onResource(resourceType.cast(r)), createParserFactory(eventType));
	}

	private Supplier<IParser> createParserFactory(EventType eventType)
	{
		return switch (eventType)
		{
			case XML -> configureParser(fhirContext::newXmlParser);
			case JSON -> configureParser(fhirContext::newJsonParser);

			default -> throw new RuntimeException("EventType " + eventType + " not supported");
		};
	}

	private Supplier<IParser> configureParser(Supplier<IParser> supplier)
	{
		return () ->
		{
			IParser p = supplier.get();
			p.setStripVersionsFromReferences(false);
			p.setOverrideResourceIdWithBundleEntryFullUrl(false);

			return p;
		};
	}
}
