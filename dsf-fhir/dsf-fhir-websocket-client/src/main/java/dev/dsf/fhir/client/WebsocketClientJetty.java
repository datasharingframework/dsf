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

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLContext;

import org.eclipse.jetty.client.Authentication;
import org.eclipse.jetty.client.BasicAuthentication;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpProxy;
import org.eclipse.jetty.client.Origin.Address;
import org.eclipse.jetty.client.transport.HttpClientTransportOverHTTP;
import org.eclipse.jetty.http.HttpField;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.io.ClientConnector;
import org.eclipse.jetty.util.component.ContainerLifeCycle;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.VirtualThreadPool;
import org.eclipse.jetty.websocket.api.Callback;
import org.eclipse.jetty.websocket.api.Callback.Completable;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.Session.Listener;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.client.WebSocketClient;
import org.hl7.fhir.r4.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class WebsocketClientJetty implements WebsocketClient
{
	private static final Logger logger = LoggerFactory.getLogger(WebsocketClientJetty.class);

	private static final int PING_HISTORY_SIZE = 10;

	private static enum State
	{
		DISCONNECTED, CONNECTING, CONNECTED, RECONNECT_WAIT, CLOSED
	}

	public static record TimeConfig(Duration connectTimeout, Duration readTimeout, Duration pingInterval,
			Duration pongTimeout, Duration reconnectDelayInitial, Duration reconnectIncrement,
			Duration reconnectDelayMax, Duration closeTimeout)
	{
		public static final TimeConfig DEFAULT = new TimeConfig(Duration.ofSeconds(2), Duration.ofSeconds(30),
				Duration.ofSeconds(20), Duration.ofSeconds(2), Duration.ofSeconds(2), Duration.ofSeconds(1),
				Duration.ofSeconds(20), Duration.ofSeconds(2));

		public TimeConfig
		{
			Objects.requireNonNull(connectTimeout, "connectTimeout");
			Objects.requireNonNull(pingInterval, "pingInterval");
			Objects.requireNonNull(pongTimeout, "pongTimeout");
			Objects.requireNonNull(reconnectDelayInitial, "reconnectDelayInitial");
			Objects.requireNonNull(reconnectIncrement, "reconnectIncrement");
			Objects.requireNonNull(reconnectDelayMax, "reconnectDelayMax");
			Objects.requireNonNull(closeTimeout, "closeTimeout");

			if (connectTimeout.isNegative() || connectTimeout.isZero())
				throw new IllegalArgumentException("connectTimeout must be positive");
			if (pingInterval.isNegative() || pingInterval.toMillis() < 1)
				throw new IllegalArgumentException("pingInterval must be at least 1 ms");
			if (pongTimeout.isNegative() || pongTimeout.isZero())
				throw new IllegalArgumentException("pongTimeout must be positive");
			if (reconnectDelayInitial.isNegative() || reconnectDelayInitial.isZero())
				throw new IllegalArgumentException("reconnectDelayInitial must be positive");
			if (reconnectIncrement.isNegative() || reconnectIncrement.isZero())
				throw new IllegalArgumentException("reconnectIncrement must be positive");
			if (reconnectDelayMax.isNegative() || reconnectDelayMax.isZero())
				throw new IllegalArgumentException("reconnectDelayMax must be positive");
			if (reconnectDelayMax.toMillis() < reconnectDelayInitial.toMillis())
				throw new IllegalArgumentException("reconnectDelayMax must be larger then reconnectDelayInitial");
		}

		@Override
		public String toString()
		{
			return "{" + (connectTimeout != null ? "connectTimeout: " + connectTimeout + ", " : "")
					+ (pingInterval != null ? "pingInterval: " + pingInterval + ", " : "")
					+ (pongTimeout != null ? "pongTimeout: " + pongTimeout + ", " : "")
					+ (reconnectDelayInitial != null ? "reconnectDelayInitial: " + reconnectDelayInitial + ", " : "")
					+ (reconnectIncrement != null ? "reconnectIncrement: " + reconnectIncrement + ", " : "")
					+ (reconnectDelayMax != null ? "reconnectDelayMax: " + reconnectDelayMax + ", " : "")
					+ (closeTimeout != null ? "closeTimeout: " + closeTimeout : "") + "}";
		}
	}

	public static record ProxyConfig(URI uri, String username, char[] password)
	{
		public ProxyConfig
		{
			Objects.requireNonNull(uri, "uri");
			if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme()))
				throw new IllegalArgumentException("uri.scheme http or https expected");

			password = password == null ? null : password.clone();
		}

		@Override
		public char[] password()
		{
			return password == null ? null : password.clone();
		}

		public HttpProxy getProxy(SslContextFactory.Client sslFactory)
		{
			return new HttpProxy(new Address(uri.getHost(), uri.getPort()),
					"https".equals(uri.getScheme()) ? sslFactory : null);
		}

		public boolean needsAuthentication()
		{
			return username != null && password != null;
		}

		public BasicAuthentication getAuthentication()
		{
			return new BasicAuthentication(uri, Authentication.ANY_REALM, username, new String(password));
		}
	}

	private static final class ConnectionContext
	{
		static final long NO_PENDING_PING = 0L;

		final long generation;
		final Session session;

		final AtomicLong pendingPing = new AtomicLong(NO_PENDING_PING);
		final AtomicReference<ScheduledFuture<?>> pingTask = new AtomicReference<>();
		final AtomicReference<ScheduledFuture<?>> pingTimeout = new AtomicReference<>();

		ConnectionContext(long generation, Session session)
		{
			this.generation = generation;
			this.session = session;
		}
	}

	private final URI uri;
	private final Endpoint endpoint;
	private final TimeConfig timeConfig;

	private final ExecutorService callbackExecutor;
	private final ScheduledExecutorService reconnectAndPingScheduler;

	private final Deque<Long> pingHistory = new ArrayDeque<>(PING_HISTORY_SIZE);

	private final Object lifecycleLock = new Object();

	// volatile for lock-free isConnected
	private volatile State state = State.DISCONNECTED;

	private ConnectionContext connection;

	private long generation;
	private int reconnectAttempts;

	private ScheduledFuture<?> reconnectTask;
	private CompletableFuture<Session> connectionFuture;
	private CompletableFuture<Void> closeFuture;

	private final ContainerLifeCycle lifeCycle = new ContainerLifeCycle();
	private final HttpClient httpClient;

	private WebSocketClient client;

	/**
	 * @param uri
	 *            not <code>null</code>, must have scheme <code>ws</code> or <code>wss</code>
	 * @param endpoint
	 *            not <code>null</code>
	 * @param userAgentValue
	 *            not <code>null</code>
	 * @param resourceType
	 *            not <code>null</code>
	 */
	public WebsocketClientJetty(URI uri, Endpoint endpoint, String userAgentValue, String resourceType)
	{
		this(uri, endpoint, userAgentValue, resourceType, TimeConfig.DEFAULT, null, null);
	}

	/**
	 * @param uri
	 *            not <code>null</code>, must have scheme <code>ws</code> or <code>wss</code>
	 * @param endpoint
	 *            not <code>null</code>
	 * @param userAgentValue
	 *            not <code>null</code>
	 * @param resourceType
	 *            not <code>null</code>
	 * @param timeConfig
	 *            not <code>null</code>
	 * @param sslContext
	 *            may be <code>null</code>, default jvm config if <code>null</code>
	 * @param proxyConfig
	 *            may be <code>null</code>, no forward proxy if <code>null</code>
	 */
	public WebsocketClientJetty(URI uri, Endpoint endpoint, String userAgentValue, String resourceType,
			TimeConfig timeConfig, SSLContext sslContext, ProxyConfig proxyConfig)
	{
		Objects.requireNonNull(uri, "uri");
		Objects.requireNonNull(endpoint, "endpoint");
		Objects.requireNonNull(timeConfig, "timeConfig");
		Objects.requireNonNull(userAgentValue, "userAgentValue");
		Objects.requireNonNull(resourceType, "resourceType");

		if (!"ws".equalsIgnoreCase(uri.getScheme()) && !"wss".equalsIgnoreCase(uri.getScheme()))
			throw new IllegalArgumentException("uri must have scheme ws or wss");

		this.uri = uri;
		this.endpoint = endpoint;
		this.timeConfig = timeConfig;

		callbackExecutor = Executors.newSingleThreadExecutor(
				Thread.ofPlatform().daemon().name("websocket-callback-" + resourceType).factory());

		reconnectAndPingScheduler = Executors.newSingleThreadScheduledExecutor(
				Thread.ofPlatform().daemon().name("websocket-scheduler-" + resourceType).factory());

		SslContextFactory.Client sslFactory = manage(new SslContextFactory.Client());
		sslFactory.setSslContext(sslContext);

		VirtualThreadPool threadPool = manage(new VirtualThreadPool());
		threadPool.setMaxConcurrentTasks(200);
		threadPool.setName("websocket-client-pool-" + resourceType + "-");

		ScheduledExecutorScheduler scheduler = manage(
				new ScheduledExecutorScheduler("websocket-scheduler-" + resourceType, true));

		ClientConnector connector = new ClientConnector();
		connector.setExecutor(threadPool);
		connector.setScheduler(scheduler);
		connector.setSslContextFactory(sslFactory);
		connector.setConnectTimeout(timeConfig.connectTimeout());
		connector.setIdleTimeout(timeConfig.readTimeout());

		httpClient = manage(new HttpClient(new HttpClientTransportOverHTTP(connector)));
		httpClient.setUserAgentField(new HttpField(HttpHeader.USER_AGENT, userAgentValue));

		if (proxyConfig != null)
		{
			httpClient.getProxyConfiguration().addProxy(proxyConfig.getProxy(sslFactory));
			if (proxyConfig.needsAuthentication())
				httpClient.getAuthenticationStore().addAuthentication(proxyConfig.getAuthentication());
		}
	}

	private <T> T manage(T t)
	{
		Objects.requireNonNull(t, "t");

		boolean managed = lifeCycle.addBean(t);
		if (!managed)
			throw new IllegalStateException("LifeCycle component of type " + t.getClass().getName() + " not managed");

		return t;
	}

	@Override
	public boolean isConnected()
	{
		return state == State.CONNECTED;
	}

	@Override
	public CompletableFuture<Void> connect()
	{
		return doConnect().thenApply(_ -> null);
	}

	private CompletableFuture<Session> doConnect()
	{
		synchronized (lifecycleLock)
		{
			if (state == State.CLOSED)
				return CompletableFuture.failedFuture(new IllegalStateException("Client is closed"));
			if (state == State.CONNECTED)
				return CompletableFuture.completedFuture(connection.session);
			if (state == State.CONNECTING)
				return connectionFuture;

			try
			{
				lifeCycle.start();
				client = new WebSocketClient(httpClient);
				client.start();
			}
			catch (Exception e)
			{
				return CompletableFuture.failedFuture(e);
			}

			cancelReconnectWithLifecycleLock();
			reconnectAttempts = 0;

			return connectWithLifecycleLock();
		}
	}

	private CompletableFuture<Session> connectWithLifecycleLock()
	{
		if (state == State.CLOSED)
			return CompletableFuture.failedFuture(new IllegalStateException("Client is closed"));
		if (state == State.CONNECTING)
			return connectionFuture;
		if (state == State.CONNECTED)
			return CompletableFuture.completedFuture(connection.session);

		state = State.CONNECTING;

		long attemptGeneration = ++generation;

		CompletableFuture<Session> result = new CompletableFuture<>();

		connectionFuture = result;

		try
		{
			callbackExecutor.execute(() -> prepareAndConnect(attemptGeneration, result));
		}
		catch (RejectedExecutionException e)
		{
			failConnectionAttempt(attemptGeneration, result, e);
		}

		return result;
	}

	private void prepareAndConnect(long attemptGeneration, CompletableFuture<Session> result)
	{
		if (!isCurrentAttempt(attemptGeneration))
			return;

		final Subscription subscription;

		try
		{
			subscription = endpoint.getSubscription();
			endpoint.beforeConnect(subscription);
		}
		catch (Exception e)
		{
			failConnectionAttempt(attemptGeneration, result, e);
			return;
		}

		if (!isCurrentAttempt(attemptGeneration))
			return;

		ListenerImpl listener = new ListenerImpl(subscription, attemptGeneration);

		try
		{
			client.connect(listener, uri).whenComplete((session, e) ->
			{
				if (e != null)
				{
					failConnectionAttempt(attemptGeneration, result, e);
					return;
				}

				if (!installConnection(attemptGeneration, session))
				{
					session.close();
					result.completeExceptionally(new IllegalStateException("Connection is no longer valid"));
				}
			});
		}
		catch (Exception e)
		{
			failConnectionAttempt(attemptGeneration, result, e);
		}
	}

	private boolean isCurrentAttempt(long attemptGeneration)
	{
		synchronized (lifecycleLock)
		{
			return state == State.CONNECTING && generation == attemptGeneration;
		}
	}

	private boolean installConnection(long attemptGeneration, Session session)
	{
		ConnectionContext newContext;

		synchronized (lifecycleLock)
		{
			if (state == State.CLOSED || generation != attemptGeneration)
				return false;
			if (connection != null)
				return connection.session == session;
			if (state != State.CONNECTING)
				return false;

			newContext = new ConnectionContext(attemptGeneration, session);

			connection = newContext;
			state = State.CONNECTED;
			reconnectAttempts = 0;

			cancelReconnectWithLifecycleLock();
		}

		logger.debug("Websocket connected (generation {})", attemptGeneration);

		startPinging(newContext);

		CompletableFuture<Session> future = connectionFuture;

		if (future != null)
			future.complete(session);

		return true;
	}

	private void failConnectionAttempt(long attemptGeneration, CompletableFuture<Session> result, Throwable e)
	{
		boolean shouldReconnect = false;

		synchronized (lifecycleLock)
		{
			if (state == State.CONNECTING && generation == attemptGeneration)
			{
				state = State.DISCONNECTED;
				connection = null;
				shouldReconnect = true;
			}
		}

		result.completeExceptionally(e);

		if (shouldReconnect)
		{
			if (!endpoint.isRetryableException(e))
			{
				logger.warn("Aborting Websocket reconnect attempts: {} - {}", e.getClass().getName(), e.getMessage());
				return;
			}

			logger.warn("Websocket connection attempt failed: {} - {}", e.getClass().getName(), e.getMessage());
			scheduleReconnect();
		}
	}

	private void disconnect(ConnectionContext context, String reason)
	{
		synchronized (lifecycleLock)
		{
			if (state == State.CLOSED || connection != context)
				return;

			connection = null;
			state = State.DISCONNECTED;
		}

		logger.debug("Websocket disconnected (generation {}): {}", context.generation, reason);

		stopPinging(context);

		CompletableFuture.supplyAsync(() ->
		{
			endpoint.onDisconnect();
			return CompletableFuture.completedFuture(null);

		}, callbackExecutor).whenComplete((_, e) ->
		{
			if (e != null)
				logger.warn("Endpoint disconnect callback failed: {} - {}", e.getClass().getName(), e.getMessage());
		});

		scheduleReconnect();
	}

	private void scheduleReconnect()
	{
		synchronized (lifecycleLock)
		{
			if (state == State.CLOSED || state == State.CONNECTED || state == State.CONNECTING
					|| state == State.RECONNECT_WAIT)
				return;

			state = State.RECONNECT_WAIT;

			int attempt = ++reconnectAttempts;

			double initialMillis = timeConfig.reconnectDelayInitial().toMillis();
			double incrementMillis = timeConfig.reconnectIncrement().toMillis();
			double maxMillis = timeConfig.reconnectDelayMax().toMillis();
			double baseMillis = Math.min(maxMillis, initialMillis + (attempt - 1L) * incrementMillis);
			long delayMillis = (long) ThreadLocalRandom.current().nextDouble(baseMillis * 0.9, baseMillis * 1.1);

			logger.debug("Reconnect attempt {} scheduled in {} ms", attempt, delayMillis);

			try
			{
				reconnectTask = reconnectAndPingScheduler.schedule(this::runReconnect, delayMillis,
						TimeUnit.MILLISECONDS);
			}
			catch (RejectedExecutionException e)
			{
				if (state != State.CLOSED)
					state = State.DISCONNECTED;

				logger.warn("Unable to schedule reconnect: {} - {}", e.getClass().getName(), e.getMessage());
			}
		}
	}

	private void runReconnect()
	{
		synchronized (lifecycleLock)
		{
			if (state != State.RECONNECT_WAIT)
				return;

			reconnectTask = null;
			state = State.DISCONNECTED;

			connectWithLifecycleLock();
		}
	}

	private void cancelReconnectWithLifecycleLock()
	{
		ScheduledFuture<?> task = reconnectTask;
		reconnectTask = null;

		if (task != null)
			task.cancel(false);
	}

	private void startPinging(ConnectionContext context)
	{
		if (!isCurrentConnection(context))
			return;

		try
		{
			ScheduledFuture<?> task = reconnectAndPingScheduler.scheduleAtFixedRate(() -> sendPing(context),
					timeConfig.pingInterval().toMillis(), timeConfig.pingInterval().toMillis(), TimeUnit.MILLISECONDS);

			context.pingTask.set(task);

			if (!isCurrentConnection(context))
			{
				ScheduledFuture<?> previous = context.pingTask.getAndSet(null);

				if (previous != null)
					previous.cancel(false);
			}
		}
		catch (RejectedExecutionException e)
		{
			logger.warn("Unable to schedule websocket pings: {} - {}", e.getClass().getName(), e.getMessage());
			disconnect(context, "Ping scheduler unavailable");
			context.session.close();
		}
	}

	private void sendPing(ConnectionContext context)
	{
		if (!isCurrentConnection(context) || !context.session.isOpen())
			return;

		long sentAt = System.nanoTime();

		if (!context.pendingPing.compareAndSet(ConnectionContext.NO_PENDING_PING, sentAt))
			return;

		try
		{
			ScheduledFuture<?> timeout = reconnectAndPingScheduler.schedule(() -> onPongTimeout(context, sentAt),
					timeConfig.pongTimeout().toMillis(), TimeUnit.MILLISECONDS);

			context.pingTimeout.set(timeout);

			if (context.pendingPing.get() != sentAt)
			{
				timeout.cancel(false);
				return;
			}
		}
		catch (RejectedExecutionException e)
		{
			context.pendingPing.compareAndSet(sentAt, ConnectionContext.NO_PENDING_PING);
			disconnect(context, "Unable to schedule pong timeout");
			context.session.close();
			return;
		}

		ByteBuffer payload = ByteBuffer.allocate(Long.BYTES);
		payload.putLong(sentAt);
		payload.flip();

		try
		{
			logger.trace("Sending ping frame");
			context.session.sendPing(payload, new Callback()
			{
				@Override
				public void succeed()
				{
					logger.trace("Ping frame sent");
				}

				@Override
				public void fail(Throwable e)
				{
					if (e != null)
					{
						logger.warn("Sending ping failed: {} - {}", e.getClass().getName(), e.getMessage());
						disconnect(context, "Ping send failed");
						context.session.close();
					}
				}
			});
		}
		catch (RuntimeException e)
		{
			logger.warn("Sending ping failed: {} - {}", e.getClass().getName(), e.getMessage());
			disconnect(context, "Ping send failed");
			context.session.close();
		}
	}

	private void onPongTimeout(ConnectionContext context, long sentAt)
	{
		if (!context.pendingPing.compareAndSet(sentAt, ConnectionContext.NO_PENDING_PING))
			return;

		context.pingTimeout.set(null);

		logger.warn("Websocket pong timeout (generation {})", context.generation);

		disconnect(context, "Pong timeout");
		context.session.close();
	}

	private void handlePong(ConnectionContext context, ByteBuffer message)
	{
		if (message.remaining() != Long.BYTES)
		{
			logger.warn("Ignoring pong with unexpected payload length: {}", message.remaining());
			return;
		}

		long echoedValue = message.getLong();

		if (!context.pendingPing.compareAndSet(echoedValue, ConnectionContext.NO_PENDING_PING))
		{
			logger.warn("Ignoring unmatched pong frame");
			return;
		}

		ScheduledFuture<?> timeout = context.pingTimeout.getAndSet(null);
		if (timeout != null)
			timeout.cancel(false);

		long elapsedNanos = System.nanoTime() - echoedValue;
		long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(elapsedNanos);

		synchronized (pingHistory)
		{
			if (pingHistory.size() == PING_HISTORY_SIZE)
				pingHistory.removeFirst();

			pingHistory.addLast(elapsedMillis);
		}

		logger.trace("Received pong frame in {} ms", elapsedMillis);
	}

	private void stopPinging(ConnectionContext context)
	{
		ScheduledFuture<?> task = context.pingTask.getAndSet(null);

		if (task != null)
			task.cancel(false);

		ScheduledFuture<?> timeout = context.pingTimeout.getAndSet(null);

		if (timeout != null)
			timeout.cancel(false);

		context.pendingPing.set(ConnectionContext.NO_PENDING_PING);
	}

	private boolean isCurrentConnection(ConnectionContext context)
	{
		synchronized (lifecycleLock)
		{
			return state == State.CONNECTED && connection == context;
		}
	}

	@Override
	public List<Long> getPingHistory()
	{
		synchronized (pingHistory)
		{
			return List.copyOf(pingHistory);
		}
	}

	@Override
	public void close()
	{
		closeAsync();
	}

	@Override
	public CompletableFuture<Void> closeAsync()
	{
		ConnectionContext oldConnection;
		CompletableFuture<Session> pendingConnection;
		CompletableFuture<Void> result;

		synchronized (lifecycleLock)
		{
			if (state == State.CLOSED)
				return closeFuture;

			state = State.CLOSED;
			generation++;

			cancelReconnectWithLifecycleLock();

			oldConnection = connection;
			connection = null;

			pendingConnection = connectionFuture;

			result = new CompletableFuture<>();
			closeFuture = result;

			try
			{
				lifeCycle.stop();
			}
			catch (Exception e)
			{
				logger.debug("Error while stopping life-cycle resources: {} - {}", e.getClass().getName(),
						e.getMessage());
			}
		}

		if (pendingConnection != null)
			pendingConnection.completeExceptionally(new IllegalStateException("Client is closed"));

		if (oldConnection != null)
			stopPinging(oldConnection);

		reconnectAndPingScheduler.shutdownNow();

		CompletableFuture<Void> socketClose;

		if (oldConnection == null || !oldConnection.session.isOpen())
			socketClose = CompletableFuture.completedFuture(null);
		else
		{
			try
			{
				Completable future = new Completable();

				oldConnection.session.close(StatusCode.SHUTDOWN, "Client shutdown", future);

				socketClose = future.handle((_, e) ->
				{
					if (e != null)
					{
						oldConnection.session.close();
						logger.debug("Websocket close failed", e);
					}
					return null;
				});
			}
			catch (RuntimeException e)
			{
				oldConnection.session.close();
				socketClose = CompletableFuture.completedFuture(null);
			}
		}

		socketClose.whenComplete((_, _) ->
		{
			callbackExecutor.shutdown();
			result.complete(null);
		});

		try
		{
			client.stop();
		}
		catch (Exception e)
		{
			logger.debug("Error while stopping websocket client: {} - {}", e.getClass().getName(), e.getMessage());
		}

		return result;
	}

	protected final class ListenerImpl implements Listener
	{
		private final Subscription subscription;
		private final String subscriptionId;
		private final long attemptGeneration;

		private final StringBuilder textBuffer = new StringBuilder();

		private boolean boundReceived;

		private ConnectionContext context;

		ListenerImpl(Subscription subscription, long attemptGeneration)
		{
			this.subscription = Objects.requireNonNull(subscription, "subscription");
			this.subscriptionId = subscription.getIdElement().getIdPart();
			this.attemptGeneration = attemptGeneration;
		}

		@Override
		public void onWebSocketOpen(Session session)
		{
			if (!installConnection(attemptGeneration, session))
			{
				session.close();
				return;
			}

			ConnectionContext current;

			synchronized (lifecycleLock)
			{
				current = connection;

				if (current == null || current.session != session)
				{
					session.close();
					return;
				}

				context = current;
			}

			CompletableFuture.supplyAsync(() ->
			{
				if (!isCurrentConnection(current))
					return CompletableFuture.completedFuture(null);

				endpoint.onOpen();

				Completable future = new Completable();

				logger.debug("Sending message: bind {}", subscriptionId);
				session.sendText("bind " + subscriptionId, future);

				return future;
			}, callbackExecutor).thenRun(session::demand).whenComplete((_, e) ->
			{

				if (e != null)
				{
					logger.warn("Websocket initialization failed: {} - {}", e.getClass().getName(), e.getMessage());

					disconnect(current, "Initialization failed");
					session.close();
				}
			});
		}

		@Override
		public void onWebSocketPartialText(String payload, boolean last)
		{
			ConnectionContext current = context;
			if (current == null || !isCurrentConnection(current))
				return;

			textBuffer.append(payload);

			if (!last)
			{
				current.session.demand();
				return;
			}

			String completeMessage = textBuffer.toString();
			textBuffer.setLength(0);

			if (!boundReceived)
			{
				if (("bound " + subscriptionId).equals(completeMessage))
				{
					logger.debug("Received message: bound {}", subscriptionId);

					boundReceived = true;
					current.session.demand();

					return;
				}

				disconnect(current, "Unexpected message before bound");
				throw new IllegalStateException("No bound message received");
			}

			logger.debug("Received message: {}", completeMessage);
			endpoint.onMessage(subscription, completeMessage).thenRun(() ->
			{
				if (isCurrentConnection(current))
					current.session.demand();
			}).whenComplete((_, e) ->
			{
				if (e != null)
				{
					logger.warn("Endpoint message callback failed: {} - {}", e.getClass().getName(), e.getMessage());
					disconnect(current, "Message callback failed");
				}
			});
		}

		@Override
		public void onWebSocketPartialBinary(ByteBuffer payload, boolean last, Callback callback)
		{
			ConnectionContext current = context;
			if (current == null || !isCurrentConnection(current))
				return;

			callback.succeed();
			current.session.close(1003, "Binary messages are not supported", Callback.NOOP);
		}

		@Override
		public void onWebSocketPing(ByteBuffer payload)
		{
			ConnectionContext current = context;
			if (current == null || !isCurrentConnection(current))
				return;

			logger.trace("Received ping frame");
			current.session.demand();
		}

		@Override
		public void onWebSocketPong(ByteBuffer payload)
		{
			ConnectionContext current = context;
			if (current == null || !isCurrentConnection(current))
				return;

			logger.trace("Received pong frame");
			handlePong(current, payload);

			current.session.demand();
		}

		@Override
		public void onWebSocketClose(int statusCode, String reason, Callback callback)
		{
			logger.debug("WebSocket closed: {} - {}", statusCode, reason);

			ConnectionContext current = context;

			if (current != null)
				disconnect(current, "Close status " + statusCode + ": " + reason);

			callback.succeed();
		}

		@Override
		public void onWebSocketError(Throwable e)
		{
			if (e instanceof ClosedChannelException)
				logger.debug("Websocket closed");
			else
			{
				logger.warn("WebSocket error: {} - {}", e.getClass().getName(), e.getMessage());

				ConnectionContext current = context;

				if (current != null)
					disconnect(current, "WebSocket error");
			}
		}
	}
}
