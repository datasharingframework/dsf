package dev.dsf.fhir;

import dev.dsf.common.jetty.Log4jInitializer;
import dev.dsf.common.jetty.PropertyJettyConfig;

public class FhirJettyServerHttps
{
	static
	{
		Log4jInitializer.initializeLog4j();
	}

	public static void main(String[] args)
	{
		new FhirServer(PropertyJettyConfig.forHttps().read()).start();
	}
}
