package dev.dsf.bpe;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import dev.dsf.bpe.config.BpeDbMigratorConfig;
import dev.dsf.bpe.config.BpeHttpsJettyConfig;
import dev.dsf.common.jetty.JettyServer;
import dev.dsf.common.jetty.Log4jInitializer;
import dev.dsf.tools.db.DbMigrator;

public final class BpeJettyServerHttps
{
	static
	{
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		Log4jInitializer.initializeLog4j();
	}

	private BpeJettyServerHttps()
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
				BpeHttpsJettyConfig.class))
		{
			JettyServer server = context.getBean(JettyServer.class);

			server.addShutdownHook();
			server.start();
		}
	}
}
