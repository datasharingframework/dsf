package dev.dsf.bpe;

import dev.dsf.common.jetty.Log4jInitializer;
import dev.dsf.common.jetty.PropertyJettyConfig;

public class BpeJettyServer
{
	static
	{
		Log4jInitializer.initializeLog4j();
	}

	public static void main(String[] args)
	{
		new BpeServer(PropertyJettyConfig.forHttp().read()).start();
	}
}
