package dev.dsf.common.config;

import java.util.function.Function;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;

public abstract class AbstractHttpsJettyConfig extends AbstractJettyConfig
{
	@Override
	protected Function<Server, Connector> apiConnector()
	{
		return httpsApiConnector();
	}
}
