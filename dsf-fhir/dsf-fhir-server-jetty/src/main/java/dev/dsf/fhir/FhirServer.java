package dev.dsf.fhir;

import java.util.stream.Stream;

import org.eclipse.jetty.websocket.jakarta.client.JakartaWebSocketShutdownContainer;
import org.eclipse.jetty.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.glassfish.jersey.servlet.init.JerseyServletContainerInitializer;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.SpringServletContainerInitializer;

import dev.dsf.common.jetty.JettyConfig;
import dev.dsf.common.jetty.JettyServer;
import dev.dsf.tools.db.DbMigrator;
import dev.dsf.tools.db.DbMigratorConfig;

public final class FhirServer extends JettyServer
{
	static
	{
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	public FhirServer(JettyConfig jettyConfig)
	{
		super("fhir-server", jettyConfig,
				Stream.of(JakartaWebSocketShutdownContainer.class, JakartaWebSocketServletContainerInitializer.class,
						JerseyServletContainerInitializer.class, SpringServletContainerInitializer.class));
	}

	@Override
	public void beforeStart()
	{
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				FhirDbMigratorConfig.class))
		{
			DbMigratorConfig config = context.getBean(DbMigratorConfig.class);
			DbMigrator dbMigrator = new DbMigrator(config);
			DbMigrator.retryOnConnectException(3, dbMigrator::migrate);
		}
	}
}
