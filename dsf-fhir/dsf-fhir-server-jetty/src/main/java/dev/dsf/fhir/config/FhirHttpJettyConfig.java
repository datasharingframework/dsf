package dev.dsf.fhir.config;

import java.util.List;

import org.eclipse.jetty.ee10.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.glassfish.jersey.servlet.init.JerseyServletContainerInitializer;
import org.springframework.web.SpringServletContainerInitializer;

import dev.dsf.common.config.AbstractHttpJettyConfig;
import jakarta.servlet.ServletContainerInitializer;

public class FhirHttpJettyConfig extends AbstractHttpJettyConfig
{
	@Override
	protected String mavenServerModuleName()
	{
		return "fhir-server";
	}

	@Override
	protected List<Class<? extends ServletContainerInitializer>> servletContainerInitializers()
	{
		return List.of(JakartaWebSocketServletContainerInitializer.class, JerseyServletContainerInitializer.class,
				SpringServletContainerInitializer.class);
	}
}
