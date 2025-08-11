package dev.dsf.fhir;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import dev.dsf.common.db.migration.DbMigrator;
import dev.dsf.common.jetty.JettyServer;
import dev.dsf.fhir.config.FhirDbMigratorConfig;
import dev.dsf.fhir.config.FhirHttpsJettyConfig;
import dev.dsf.fhir.logging.FhirLog4jInitializer;

public final class FhirJettyServerHttps
{
	static
	{
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		new FhirLog4jInitializer().initializeLog4j();
	}

	private FhirJettyServerHttps()
	{
	}

	public static void main(String[] args)
	{
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				FhirDbMigratorConfig.class))
		{
			DbMigrator migrator = context.getBean(DbMigrator.class);
			DbMigrator.retryOnConnectException(3, migrator::migrate);
		}

		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				FhirHttpsJettyConfig.class))
		{
			JettyServer server = context.getBean(JettyServer.class);

			server.addShutdownHook();
			server.start();
		}
	}
}
