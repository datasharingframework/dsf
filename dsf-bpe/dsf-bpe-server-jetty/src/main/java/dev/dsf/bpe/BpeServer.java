package dev.dsf.bpe;

import java.util.stream.Stream;

import org.glassfish.jersey.servlet.init.JerseyServletContainerInitializer;
import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.web.SpringServletContainerInitializer;

import dev.dsf.common.jetty.JettyConfig;
import dev.dsf.common.jetty.JettyServer;
import dev.dsf.tools.db.DbMigrator;
import dev.dsf.tools.db.DbMigratorConfig;

public final class BpeServer extends JettyServer
{
	static
	{
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();
	}

	public BpeServer(JettyConfig jettyConfig)
	{
		super("bpe-server", jettyConfig,
				Stream.of(JerseyServletContainerInitializer.class, SpringServletContainerInitializer.class));
	}

	@Override
	public void beforeStart()
	{
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BpeDbMigratorConfig.class))
		{
			DbMigratorConfig config = context.getBean(DbMigratorConfig.class);
			DbMigrator dbMigrator = new DbMigrator(config);
			DbMigrator.retryOnConnectException(3, dbMigrator::migrate);
		}
	}
}
