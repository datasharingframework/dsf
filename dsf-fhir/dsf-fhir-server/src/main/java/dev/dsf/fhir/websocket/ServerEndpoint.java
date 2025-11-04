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
package dev.dsf.fhir.websocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.FhirServerRoleImpl;
import dev.dsf.fhir.subscription.WebSocketSubscriptionManager;
import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler.Whole;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;

public class ServerEndpoint extends Endpoint implements InitializingBean, DisposableBean
{
	private static final Logger logger = LoggerFactory.getLogger(ServerEndpoint.class);

	public static final String PATH = "/ws";
	public static final String USER_PROPERTY = ServerEndpoint.class.getName() + ".user";
	private static final String PINGER_PROPERTY = ServerEndpoint.class.getName() + ".pinger";
	private static final String BIND_MESSAGE_START = "bind ";

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

	private final WebSocketSubscriptionManager subscriptionManager;

	public ServerEndpoint(WebSocketSubscriptionManager subscriptionManager)
	{
		this.subscriptionManager = subscriptionManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(subscriptionManager, "subscriptionManager");
	}

	@Override
	public void onOpen(Session session, EndpointConfig config)
	{
		Principal principal = session.getUserPrincipal();
		if (principal == null || !(principal instanceof Identity) || !((Identity) principal).getDsfRoles().stream()
				.map(DsfRole::name).anyMatch(FhirServerRoleImpl.Operation.WEBSOCKET.name()::equals))
		{
			logger.warn("No user in session or user is missing role {}, closing websocket, session {}",
					FhirServerRoleImpl.Operation.WEBSOCKET, session.getId());
			try
			{
				session.close(new CloseReason(CloseCodes.VIOLATED_POLICY, "Forbidden"));
			}
			catch (IOException e)
			{
				logger.debug("Error while closing websocket, session {}", session.getId(), e);
				logger.warn("Error while closing websocket, session {}: {} - {}", session.getId(),
						e.getClass().getName(), e.getMessage());
			}

			return;
		}

		logger.info("Websocket open, session {}, identity '{}'", session.getId(), principal.getName());

		session.addMessageHandler(new Whole<String>() // don't use lambda
		{
			@Override
			public void onMessage(String message)
			{
				logger.debug("Websocket message received, session {}: {}", session.getId(), message);

				if (message != null && !message.isBlank() && message.startsWith(BIND_MESSAGE_START))
				{
					String subscriptionIdPart = message.substring(BIND_MESSAGE_START.length());

					logger.debug("Websocket bind message received, session {}, subscription: {}", session.getId(),
							subscriptionIdPart);
					subscriptionManager.bind((Identity) principal, session, subscriptionIdPart);
				}
			}
		});

		ScheduledFuture<?> pinger = scheduler.scheduleWithFixedDelay(() -> ping(session), 28, 28, TimeUnit.SECONDS);
		session.getUserProperties().put(PINGER_PROPERTY, pinger);
	}

	private void ping(Session session)
	{
		byte[] send = new byte[32];
		ThreadLocalRandom.current().nextBytes(send);

		session.addMessageHandler(new Whole<PongMessage>()
		{
			@Override
			public void onMessage(PongMessage message)
			{
				byte[] read = new byte[32];
				message.getApplicationData().get(read);
				logger.trace("Pong frame received, session {}: {}", session.getId(), Hex.encodeHexString(read));

				if (!Arrays.equals(send, read))
					logger.warn("Ping frame data not equal to pong frame data, session {}: {} vs. {}", session.getId(),
							Hex.encodeHexString(send), Hex.encodeHexString(read));

				session.removeMessageHandler(this);
			}
		});

		try
		{
			logger.trace("Sending ping frame, session {}: {}", session.getId(), Hex.encodeHexString(send));
			session.getAsyncRemote().sendPing(ByteBuffer.wrap(send));
		}
		catch (IllegalArgumentException | IOException e)
		{
			logger.debug("Error while sending ping frame, session {}", session.getId(), e);
			logger.warn("Error while sending ping frame, session {}: {} - {}", session.getId(), e.getClass().getName(),
					e.getMessage());
		}
	}

	@Override
	public void onClose(Session session, CloseReason closeReason)
	{
		logger.info("Websocket closed, session {}: {} - {}", session.getId(), closeReason.getCloseCode().getCode(),
				closeReason.getReasonPhrase());
		subscriptionManager.close(session.getId());

		ScheduledFuture<?> pinger = (ScheduledFuture<?>) session.getUserProperties().get(PINGER_PROPERTY);
		if (pinger != null)
			pinger.cancel(true);
	}

	@Override
	public void onError(Session session, Throwable throwable)
	{
		if (throwable == null)
			logger.info("Websocket closed with error, session {}: unknown error", session.getId());
		else
		{
			logger.debug("Websocket closed with error, session {}", session.getId(), throwable);
			logger.info("Websocket closed with error, session {}: {} - {}", session.getId(),
					throwable.getClass().getName(), getMessages(throwable));
		}
	}

	private String getMessages(Throwable e)
	{
		StringBuilder b = new StringBuilder();
		if (e != null)
		{
			if (e.getMessage() != null)
				b.append(e.getMessage());

			Throwable cause = e.getCause();
			while (cause != null)
			{
				if (cause.getMessage() != null)
				{
					b.append(' ');
					b.append(cause.getMessage());
				}

				cause = cause.getCause();
			}
		}
		return b.toString();
	}


	@Override
	public void destroy() throws Exception
	{
		scheduler.shutdown();
		try
		{
			if (!scheduler.awaitTermination(60, TimeUnit.SECONDS))
			{
				scheduler.shutdownNow();
				if (!scheduler.awaitTermination(60, TimeUnit.SECONDS))
					logger.warn("EventEndpoint scheduler did not terminate");
			}
		}
		catch (InterruptedException ie)
		{
			scheduler.shutdownNow();
			Thread.currentThread().interrupt();
		}
	}
}