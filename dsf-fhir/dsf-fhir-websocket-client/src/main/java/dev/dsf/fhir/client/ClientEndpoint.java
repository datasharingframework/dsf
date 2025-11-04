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
package dev.dsf.fhir.client;

import java.util.EnumSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.parser.IParser;
import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.Session;

public class ClientEndpoint extends Endpoint
{
	private static final Logger logger = LoggerFactory.getLogger(ClientEndpoint.class);

	private final Runnable reconnector;
	private final String subscriptionIdPart;

	public ClientEndpoint(Runnable reconnector, String subscriptionIdPart)
	{
		this.reconnector = reconnector;
		this.subscriptionIdPart = subscriptionIdPart;
	}

	private Supplier<IParser> parserFactory;
	private Consumer<Resource> domainResourceHandler;
	private Consumer<String> pingHandler;

	@Override
	public void onOpen(Session session, EndpointConfig config)
	{
		logger.info("Websocket open, session {}", session.getId());

		session.addMessageHandler(new MessageHandler.Whole<String>() // don't use lambda
		{
			private boolean boundReceived;

			@Override
			public void onMessage(String message)
			{
				logger.debug("Websocket message received, session {}: {}", session.getId(), message);

				if (("bound " + subscriptionIdPart).equals(message))
				{
					logger.debug("Bound received");
					boundReceived = true;
					return;
				}

				if (boundReceived)
				{
					try
					{
						if (pingHandler != null && ("ping " + subscriptionIdPart).equals(message))
							pingHandler.accept(message);
						else if (domainResourceHandler != null && parserFactory != null)
							domainResourceHandler.accept((Resource) parserFactory.get().parseResource(message));
					}
					catch (Throwable e)
					{
						logger.debug("Error while handling message, session {}", session.getId(), e);
						logger.error("Error while handling message, session {}: {} - {}", session.getId(),
								e.getClass().getName(), e.getMessage());
					}
				}
			}
		});

		session.getAsyncRemote().sendText("bind " + subscriptionIdPart);
	}

	@Override
	public void onClose(Session session, CloseReason closeReason)
	{
		logger.warn("Websocket closed, session {}: {} - {}", session.getId(), closeReason.getCloseCode().getCode(),
				closeReason.getReasonPhrase());

		if (EnumSet.of(CloseReason.CloseCodes.CANNOT_ACCEPT, CloseReason.CloseCodes.CLOSED_ABNORMALLY)
				.contains(closeReason.getCloseCode()))
		{
			logger.info("Trying to reconnect websocket");
			reconnector.run();
		}
	}

	@Override
	public void onError(Session session, Throwable throwable)
	{
		if (throwable == null)
			logger.info("Websocket closed with error, session {}: unknown error", session.getId());
		else
		{
			logger.debug("Websocket closed with error, session {}", session.getId(), throwable);
			logger.warn("Websocket closed with error, session {}: {} - {}", session.getId(),
					throwable.getClass().getName(), throwable.getMessage());
		}
	}

	public void setResourceHandler(Consumer<Resource> handler, Supplier<IParser> parser)
	{
		domainResourceHandler = handler;
		parserFactory = parser;
		pingHandler = null;
	}

	public void setPingHandler(Consumer<String> handler)
	{
		domainResourceHandler = null;
		parserFactory = null;
		pingHandler = handler;
	}
}
