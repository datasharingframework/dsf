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

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authentication.FhirServerRole;
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
		logger.debug("onOpen session: {}", session.getId());

		Principal principal = session.getUserPrincipal();


		if (principal == null || !(principal instanceof Identity)
				|| !((Identity) principal).hasRole(FhirServerRole.WEBSOCKET))
		{
			logger.warn("No user in session or user is missing role {}, closing websocket: {}",
					FhirServerRole.WEBSOCKET, session.getId());
			try
			{
				session.close(new CloseReason(CloseCodes.VIOLATED_POLICY, "Forbidden"));
			}
			catch (IOException e)
			{
				logger.warn("Error while closing websocket", e);
			}

			return;
		}

		session.addMessageHandler(new Whole<String>() // don't use lambda
		{
			@Override
			public void onMessage(String message)
			{
				logger.debug("onMessage session: {}, message: {}", session.getId(), message);

				if (message != null && !message.isBlank() && message.startsWith(BIND_MESSAGE_START))
				{
					logger.debug("Websocket bind message received: {}", message);
					subscriptionManager.bind((Identity) principal, session,
							message.substring(BIND_MESSAGE_START.length()));
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
				logger.trace("onPongMessage {} from session {}", Hex.encodeHexString(read), session.getId());

				if (!Arrays.equals(send, read))
					logger.warn("ping data not equal to pong data {} != {}", Hex.encodeHexString(send),
							Hex.encodeHexString(read));

				session.removeMessageHandler(this);
			}
		});

		try
		{
			logger.trace("sending ping {} to session {}", Hex.encodeHexString(send), session.getId());
			session.getAsyncRemote().sendPing(ByteBuffer.wrap(send));
		}
		catch (IllegalArgumentException | IOException e)
		{
			logger.warn("Error while sending ping to session with id " + session.getId(), e);
		}
	}

	@Override
	public void onClose(Session session, CloseReason closeReason)
	{
		logger.debug("onClose " + session.getId());
		subscriptionManager.close(session.getId());

		ScheduledFuture<?> pinger = (ScheduledFuture<?>) session.getUserProperties().get(PINGER_PROPERTY);
		if (pinger != null)
			pinger.cancel(true);
	}

	@Override
	public void onError(Session session, Throwable thr)
	{
		logger.info("onError {} - {}", session.getId(),
				thr != null ? (thr.getClass().getName() + ": " + thr.getMessage()) : "");
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