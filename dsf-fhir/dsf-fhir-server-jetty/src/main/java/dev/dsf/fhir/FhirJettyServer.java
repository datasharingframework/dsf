package dev.dsf.fhir;

import java.io.IOException;

import dev.dsf.common.jetty.Log4jInitializer;
import dev.dsf.common.jetty.PropertyJettyConfig;

public class FhirJettyServer
{
	static
	{
		Log4jInitializer.initializeLog4j();
	}

	public static void main(String[] args) throws IOException
	{
		new FhirServer(PropertyJettyConfig.forHttp().read()).start();
	}
}
