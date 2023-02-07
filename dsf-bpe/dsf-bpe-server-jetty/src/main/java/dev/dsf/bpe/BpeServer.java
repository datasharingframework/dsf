package dev.dsf.bpe;

import static de.rwh.utils.jetty.JettyServer.httpConfiguration;
import static de.rwh.utils.jetty.JettyServer.start;
import static de.rwh.utils.jetty.JettyServer.statusCodeOnlyErrorHandler;
import static de.rwh.utils.jetty.JettyServer.webInfClassesDirs;
import static de.rwh.utils.jetty.JettyServer.webInfJars;
import static de.rwh.utils.jetty.PropertiesReader.read;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.servlet.Filter;
import javax.servlet.SessionTrackingMode;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConfiguration.Customizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.glassfish.jersey.servlet.init.JerseyServletContainerInitializer;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.SpringServletContainerInitializer;
import org.springframework.web.filter.CorsFilter;

import de.rwh.utils.jetty.JettyServer;
import de.rwh.utils.jetty.Log4jInitializer;
import dev.dsf.bpe.webservice.StatusService;
import dev.dsf.tools.db.DbMigrator;
import dev.dsf.tools.db.DbMigratorConfig;

public final class BpeServer
{
	static
	{
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	public static void startHttpServer()
	{
		startServer(JettyServer::forwardedSecureRequestCustomizer, JettyServer::httpConnector);
	}

	public static void startHttpsServer()
	{
		startServer(JettyServer::secureRequestCustomizer, JettyServer::httpsConnector);
	}

	private static void startServer(Function<Properties, Customizer> customizerBuilder,
			BiFunction<HttpConfiguration, Properties, Function<Server, ServerConnector>> connectorBuilder)
	{
		Properties properties = read(Paths.get("conf/jetty.properties"), StandardCharsets.UTF_8);

		Log4jInitializer.initializeLog4j(properties);

		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BpeDbMigratorConfig.class))
		{
			DbMigratorConfig config = context.getBean(DbMigratorConfig.class);
			DbMigrator dbMigrator = new DbMigrator(config);
			DbMigrator.retryOnConnectException(3, dbMigrator::migrate);
		}

		HttpConfiguration httpConfiguration = httpConfiguration(customizerBuilder.apply(properties));
		Function<Server, ServerConnector> connector = connectorBuilder.apply(httpConfiguration, properties);
		Function<Server, ServerConnector> statusConnector = JettyServer.httpConnector(httpConfiguration(), "localhost",
				StatusService.PORT);
		List<Function<Server, ServerConnector>> connectors = Arrays.asList(connector, statusConnector);

		Predicate<String> filter = s -> s.contains("bpe-server");
		Stream<String> webInfClassesDirs = webInfClassesDirs(filter);
		Stream<String> webInfJars = webInfJars(filter);

		List<Class<?>> initializers = Arrays.asList(SpringServletContainerInitializer.class,
				JerseyServletContainerInitializer.class);

		ErrorHandler errorHandler = statusCodeOnlyErrorHandler();

		List<Class<? extends Filter>> filters = new ArrayList<>();
		if (Boolean.parseBoolean(properties.getProperty("jetty.cors.enable")))
			filters.add(CorsFilter.class);

		@SuppressWarnings("unchecked")
		JettyServer server = new JettyServer(connectors, errorHandler, "/bpe", initializers, null, webInfClassesDirs,
				webInfJars, filters.toArray(new Class[filters.size()]));

		server.getWebAppContext().addEventListener(new SessionInvalidator());
		server.getWebAppContext().getSessionHandler()
				.setSessionTrackingModes(Collections.singleton(SessionTrackingMode.SSL));

		start(server);
	}

	private BpeServer()
	{
	}
}
