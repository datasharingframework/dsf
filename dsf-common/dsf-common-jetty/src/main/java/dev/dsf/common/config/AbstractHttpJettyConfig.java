package dev.dsf.common.config;

import java.util.function.Function;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

public abstract class AbstractHttpJettyConfig extends AbstractJettyConfig
{
	@Override
	protected Function<Server, ServerConnector> apiConnector()
	{
		return httpApiConnector();
	}
}
