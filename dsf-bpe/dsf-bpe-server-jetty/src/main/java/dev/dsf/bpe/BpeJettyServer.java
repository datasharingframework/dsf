package dev.dsf.bpe;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import dev.dsf.bpe.config.BpeDbMigratorConfig;
import dev.dsf.bpe.config.BpeHttpJettyConfig;
import dev.dsf.common.db.migration.DbMigrator;
import dev.dsf.common.jetty.JettyServer;
import dev.dsf.common.logging.Log4jInitializer;

public final class BpeJettyServer
{
	static
	{
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		new Log4jInitializer("bpe").initializeLog4j();
	}

	private BpeJettyServer()
	{
	}

	public static void main(String[] args)
	{
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BpeDbMigratorConfig.class))
		{
			DbMigrator migrator = context.getBean(DbMigrator.class);
			DbMigrator.retryOnConnectException(3, migrator::migrate);
		}

		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				BpeHttpJettyConfig.class))
		{
			JettyServer server = context.getBean(JettyServer.class);

			server.addShutdownHook();
			server.start();
		}
	}
}
