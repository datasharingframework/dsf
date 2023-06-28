package dev.dsf.fhir;

import org.slf4j.bridge.SLF4JBridgeHandler;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import dev.dsf.common.jetty.JettyServer;
import dev.dsf.common.jetty.Log4jInitializer;
import dev.dsf.fhir.config.FhirDbMigratorConfig;
import dev.dsf.fhir.config.FhirHttpJettyConfig;
import dev.dsf.tools.db.DbMigrator;

public class FhirJettyServer
{
	static
	{
		SLF4JBridgeHandler.removeHandlersForRootLogger();
		SLF4JBridgeHandler.install();

		Log4jInitializer.initializeLog4j();
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
				FhirHttpJettyConfig.class))
		{
			JettyServer server = context.getBean(JettyServer.class);
			server.start();
		}
	}
}
