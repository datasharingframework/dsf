package dev.dsf.common.jetty;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jetty.annotations.AnnotationConfiguration;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.webapp.Configuration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.ClientCertificateAuthenticator;
import dev.dsf.common.auth.DsfLoginService;
import dev.dsf.common.auth.DsfSecurityHandler;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;

public class JettyServer
{
	private static final Logger logger = LoggerFactory.getLogger(JettyServer.class);

	private final Server server;
	private final WebAppContext webAppContext;

	public JettyServer(String serverModule, JettyConfig config,
			Stream<Class<? extends ServletContainerInitializer>> initializers)
	{
		config = new EnvJettyConfig(config);

		server = new Server(threadPool());
		server.addConnector(config.createConnector(server));
		server.addConnector(config.createStatusConnector(server));

		webAppContext = webAppContext(serverModule, config, initializers);

		SecurityHandler securityHandler = new DsfSecurityHandler(new DsfLoginService(webAppContext),
				new ClientCertificateAuthenticator(
						config.getContextPath()
								.orElseThrow(JettyConfig.propertyNotDefined(JettyConfig.PROPERTY_JETTY_CONTEXT_PATH)),
						config.getStatusPort()
								.orElseThrow(JettyConfig.propertyNotDefined(JettyConfig.PROPERTY_JETTY_STATUS_PORT)),
						config.getClientTrustStore().orElseThrow(
								JettyConfig.propertyNotDefined(JettyConfig.PROPERTY_JETTY_CLIENT_TRUSTSTORE_PEM))));
		securityHandler.setHandler(webAppContext);

		server.setHandler(securityHandler);
		server.setErrorHandler(statusCodeOnlyErrorHandler());
	}

	private QueuedThreadPool threadPool()
	{
		QueuedThreadPool threadPool = new QueuedThreadPool();
		threadPool.setName("jetty-server");
		return threadPool;
	}

	private WebAppContext webAppContext(String serverModule, JettyConfig config,
			Stream<Class<? extends ServletContainerInitializer>> initializers)
	{
		String[] classPath = classPath();

		WebAppContext context = new WebAppContext();
		config.getAllProperties().forEach(context::setInitParameter);
		context.getServerClassMatcher().exclude(initializers.map(Class::getName).toArray(String[]::new));
		context.setContextPath(config.getContextPath()
				.orElseThrow(JettyConfig.propertyNotDefined(JettyConfig.PROPERTY_JETTY_CONTEXT_PATH)));
		context.setLogUrlOnStart(true);
		context.setThrowUnavailableOnStartupException(true);
		context.setConfigurations(new Configuration[] { new AnnotationConfiguration() });
		context.getMetaData().setWebInfClassesResources(Stream.of(classPath).filter(e -> e.contains(serverModule))
				.map(Paths::get).map(Resource::newResource).toList());
		context.addEventListener(new SessionInvalidator());

		logger.debug("Java classpath: {}", Arrays.toString(classPath));
		logger.debug("Resources for jetty: {}", context.getMetaData().getWebInfClassesResources());
		logger.debug("Init parameters: {}", clean(context.getInitParams()));

		return context;
	}

	private String clean(Map<String, String> initParams)
	{
		return initParams.entrySet().stream()
				.map(e -> e.getKey() != null && e.getValue() != null
						&& (e.getKey().contains("password") || e.getKey().contains("PASSWORD")) ? (e.getKey() + ": ***")
								: (e.getKey() + ": " + e.getValue()))
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
			protected void writeErrorPage(jakarta.servlet.http.HttpServletRequest request, java.io.Writer writer,
					int code, String message, boolean showStacks) throws java.io.IOException
			{
				logger.warn("Error {}: {}", code, message);
			}
		};
	}

	public final void start()
	{
		try
		{
			beforeStart();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}

		Runtime.getRuntime().addShutdownHook(new Thread(this::stop));

		try
		{
			logger.info("Starting jetty server ...");
			server.start();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	public void beforeStart()
	{
	}

	public final void stop()
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
	public final ServletContext getServletContext()
	{
		return webAppContext == null ? null : webAppContext.getServletContext();
	}
}
