package dev.dsf.common.jetty;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ServerSocketChannel;
import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jetty.ee10.annotations.AnnotationConfiguration;
import org.eclipse.jetty.ee10.webapp.Configuration;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.ForwardedRequestCustomizer;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.resource.PathResourceFactory;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.hsheilbronn.mi.utils.crypto.cert.CertificateFormatter.X500PrincipalFormat;
import de.hsheilbronn.mi.utils.crypto.keystore.KeyStoreFormatter;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;

public final class JettyServer
{
	private static final Logger logger = LoggerFactory.getLogger(JettyServer.class);

	private static HttpConnectionFactory httpConnectionFactory(Customizer... customizers)
	{
		HttpConfiguration httpConfiguration = new HttpConfiguration();
		httpConfiguration.setSendServerVersion(false);
		httpConfiguration.setSendXPoweredBy(false);
		httpConfiguration.setSendDateHeader(false);

		Arrays.stream(customizers).forEach(httpConfiguration::addCustomizer);

		return new HttpConnectionFactory(httpConfiguration);
	}

	public static ServerSocketChannel serverSocketChannel(String hostname)
	{
		InetSocketAddress bindAddress = hostname == null ? new InetSocketAddress(0)
				: new InetSocketAddress(hostname, 0);
		ServerSocketChannel serverChannel = null;

		try
		{
			serverChannel = ServerSocketChannel.open();
			serverChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
			serverChannel.bind(bindAddress);

			return serverChannel;
		}
		catch (IOException e)
		{
			try
			{
				if (serverChannel != null)
					serverChannel.close();
			}
			catch (IOException e1)
			{
				e.addSuppressed(e1);
			}

			throw new RuntimeException("Failed to bind to " + bindAddress, e);
		}
	}

	public static Function<Server, ServerConnector> statusConnector(String host, int port)
	{
		return server ->
		{
			ServerConnector connector = new ServerConnector(server, httpConnectionFactory());
			connector.setHost(host);
			connector.setPort(port);

			return connector;
		};
	}

	public static Function<Server, ServerConnector> statusConnector(ServerSocketChannel channel)
	{
		return server ->
		{
			try
			{
				ServerConnector connector = new ServerConnector(server, httpConnectionFactory());
				connector.open(channel);
				setHostAndPort(channel, connector);

				return connector;
			}
			catch (IOException e)
			{
				logger.debug("Unable to open server socket channel", e);
				logger.warn("Unable to open server socket channel: {} - {}", e.getClass().getName(), e.getMessage());

				throw new RuntimeException(e);
			}
		};
	}

	public static Function<Server, ServerConnector> httpConnector(String host, int port,
			String clientCertificateHeaderName)
	{
		return server ->
		{
			ServerConnector connector = new ServerConnector(server,
					httpConnectionFactory(new ForwardedRequestCustomizer(),
							new ForwardedSecureRequestCustomizer(clientCertificateHeaderName)));
			connector.setHost(host);
			connector.setPort(port);

			return connector;
		};
	}

	public static Function<Server, ServerConnector> httpConnector(ServerSocketChannel channel,
			String clientCertificateHeaderName)
	{
		return server ->
		{
			try
			{
				ServerConnector connector = new ServerConnector(server,
						httpConnectionFactory(new ForwardedRequestCustomizer(),
								new ForwardedSecureRequestCustomizer(clientCertificateHeaderName)));
				connector.open(channel);
				setHostAndPort(channel, connector);

				return connector;
			}
			catch (IOException e)
			{
				logger.debug("Unable to open server socket channel", e);
				logger.warn("Unable to open server socket channel: {} - {}", e.getClass().getName(), e.getMessage());

				throw new RuntimeException(e);
			}
		};
	}

	public static Function<Server, ServerConnector> httpsConnector(ServerSocketChannel channel,
			KeyStore clientCertificateTrustStore, KeyStore serverCertificateKeyStore, char[] keyStorePassword,
			boolean needClientAuth)
	{
		return server ->
		{
			try
			{
				ServerConnector connector = new ServerConnector(
						server, sslConnectionFactory(clientCertificateTrustStore, serverCertificateKeyStore,
								keyStorePassword, needClientAuth),
						httpConnectionFactory(new SecureRequestCustomizer()));
				connector.open(channel);
				setHostAndPort(channel, connector);

				return connector;
			}
			catch (IOException e)
			{
				logger.debug("Unable to open server socket channel", e);
				logger.warn("Unable to open server socket channel: {} - {}", e.getClass().getName(), e.getMessage());

				throw new RuntimeException(e);
			}
		};
	}

	public static Function<Server, ServerConnector> httpsConnector(String host, int port,
			KeyStore clientCertificateTrustStore, KeyStore serverCertificateKeyStore, char[] keyStorePassword,
			boolean needClientAuth)
	{
		return server ->
		{
			ServerConnector connector = new ServerConnector(server, sslConnectionFactory(clientCertificateTrustStore,
					serverCertificateKeyStore, keyStorePassword, needClientAuth),
					httpConnectionFactory(new SecureRequestCustomizer()));
			connector.setHost(host);
			connector.setPort(port);

			return connector;
		};
	}

	private static void setHostAndPort(ServerSocketChannel channel, ServerConnector connector) throws IOException
	{
		SocketAddress address = channel.getLocalAddress();
		if (address != null && address instanceof InetSocketAddress i && i.getAddress() != null
				&& i.getAddress().getHostAddress() != null)
		{
			connector.setHost(i.getAddress().getHostAddress());
			connector.setPort(i.getPort());
		}
	}

	private static SslConnectionFactory sslConnectionFactory(KeyStore clientCertificateTrustStore, KeyStore keyStore,
			char[] keyStorePassword, boolean needClientAuth)
	{
		logCertificateConfig(clientCertificateTrustStore, keyStore);

		SslContextFactory.Server sslContextFactory = new SslContextFactory.Server()
		{
			@Override
			protected KeyStore loadTrustStore(Resource resource) throws Exception
			{
				return getTrustStore();
			}
		};

		sslContextFactory.setKeyStore(keyStore);
		sslContextFactory.setKeyStorePassword(String.valueOf(keyStorePassword));

		sslContextFactory.setTrustStore(clientCertificateTrustStore);
		if (needClientAuth)
			sslContextFactory.setNeedClientAuth(true);
		else
			sslContextFactory.setWantClientAuth(true);

		return new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString());
	}

	private static void logCertificateConfig(KeyStore trustStore, KeyStore keyStore)
	{
		if (!logger.isDebugEnabled())
			return;

		if (trustStore != null)
			logger.debug("Using trust store for https connector with: {}", getCertificateSubjects(trustStore));

		if (keyStore != null)
			logger.debug("Using key store for https connector with: {}", getCertificateSubjects(keyStore));
	}

	private static String getCertificateSubjects(KeyStore keyStore)
	{
		return KeyStoreFormatter.toSubjectsFromCertificates(keyStore, X500PrincipalFormat.RFC1779).values().stream()
				.collect(Collectors.joining("; ", "[", "]"));
	}

	private final Server server;
	private final WebAppContext webAppContext;

	private final ServerConnector apiConnector;
	private final ServerConnector statusConnector;

	public JettyServer(Function<Server, ServerConnector> apiConnectorProvider,
			Function<Server, ServerConnector> statusConnectorProvider, String mavenServerModuleName, String contextPath,
			List<Class<? extends ServletContainerInitializer>> servletContainerInitializers,
			Map<String, String> initParameters, BiConsumer<WebAppContext, Supplier<Integer>> securityHandlerConfigurer)
	{
		server = new Server(threadPool());
		apiConnector = apiConnectorProvider.apply(server);
		server.addConnector(apiConnector);
		statusConnector = statusConnectorProvider.apply(server);
		server.addConnector(statusConnector);

		webAppContext = webAppContext(mavenServerModuleName, contextPath, servletContainerInitializers, initParameters);

		securityHandlerConfigurer.accept(webAppContext, statusConnector::getLocalPort);

		server.setHandler(webAppContext);
		server.setErrorHandler(statusCodeOnlyErrorHandler());
	}

	private QueuedThreadPool threadPool()
	{
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setName("jetty-server");
		return threadPool;
	}

	private WebAppContext webAppContext(String serverMavenModuleName, String contextPath,
			List<Class<? extends ServletContainerInitializer>> initializers, Map<String, String> initParameters)
	{
		WebAppContext context = new WebAppContext();

		initParameters.forEach(context::setInitParameter);

		String[] classPath = classPath();
		context.getHiddenClassMatcher().exclude(initializers.stream().map(Class::getName).toArray(String[]::new));

		context.setContextPath(contextPath);
		context.setLogUrlOnStart(true);
		context.setThrowUnavailableOnStartupException(true);
		context.setConfigurations(new Configuration[] { new AnnotationConfiguration() });

		PathResourceFactory resourceFactory = new PathResourceFactory();
		context.getMetaData().setWebInfClassesResources(Stream.of(classPath)
				.filter(e -> e.contains(serverMavenModuleName)).map(resourceFactory::newResource).toList());

		context.setErrorHandler(statusCodeOnlyErrorHandler());

		logger.debug("Java classpath: {}", Arrays.toString(classPath));
		logger.debug("Resources for jetty: {}", context.getMetaData().getWebInfClassesResources());
		logger.debug("Init parameters: {}", clean(context.getInitParams()));

		return context;
	}

	private String clean(Map<String, String> initParams)
	{
		return initParams.entrySet().stream()
				.map(e -> e.getKey() != null && e.getValue() != null
						&& (e.getKey().toLowerCase(Locale.ENGLISH).endsWith("password")
								|| e.getKey().toLowerCase(Locale.ENGLISH).endsWith("secret")) ? (e.getKey() + ": ***")
										: (e.getKey() + ": " + e.getValue().replace("\n", "\\n")))
				.collect(Collectors.joining(", ", "{", "}"));
	}

	private String[] classPath()
	{
		return System.getProperty("java.class.path").split(System.getProperty("path.separator"));
	}

	private ErrorHandler statusCodeOnlyErrorHandler()
	{
		return new ErrorHandler()
		{
			@Override
			protected void generateResponse(Request request, Response response, int code, String message,
					Throwable cause, Callback callback) throws IOException
			{
				logger.info("Error {}: {}", code, message);
				callback.succeeded();
			}
		};
	}

	public void start()
	{
		try
		{
			logger.info("Starting jetty server ...");
			server.start();
		}
		catch (Exception e)
		{
			try
			{
				stop();
			}
			catch (Exception e1)
			{
				e.addSuppressed(e1);
			}

			if (e instanceof RuntimeException r)
				throw r;
			else
				throw new RuntimeException(e);
		}
	}

	public void addShutdownHook()
	{
		Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
	}

	public void stop()
	{
		logger.info("Stopping jetty server ...");
		try
		{
			server.stop();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	/**
	 * @return <code>null</code> if server not started or web application failed to start
	 */
	public ServletContext getServletContext()
	{
		return webAppContext == null ? null : webAppContext.getServletContext();
	}

	/**
	 * @return assigned api port or <code>-1</code> or <code>-2</code>
	 * @see ServerConnector#getLocalPort()
	 */
	public int getApiPort()
	{
		return apiConnector.getLocalPort();
	}

	/**
	 * @return assigned status port or <code>-1</code> or <code>-2</code>
	 * @see ServerConnector#getLocalPort()
	 */
	public int getStatusPort()
	{
		return statusConnector.getLocalPort();
	}
}
