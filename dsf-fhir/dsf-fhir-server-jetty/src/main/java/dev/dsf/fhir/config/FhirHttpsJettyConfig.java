package dev.dsf.fhir.config;

import java.util.Arrays;
import java.util.List;

import org.eclipse.jetty.websocket.jakarta.client.JakartaWebSocketShutdownContainer;
import org.eclipse.jetty.websocket.jakarta.server.config.JakartaWebSocketServletContainerInitializer;
import org.glassfish.jersey.servlet.init.JerseyServletContainerInitializer;
import org.springframework.web.SpringServletContainerInitializer;

import dev.dsf.common.config.AbstractHttpsJettyConfig;
import jakarta.servlet.ServletContainerInitializer;

public class FhirHttpsJettyConfig extends AbstractHttpsJettyConfig
{
	@Override
	protected String mavenServerModuleName()
	{
		return "fhir-server";
	}

	@Override
	protected List<Class<? extends ServletContainerInitializer>> servletContainerInitializers()
	{
		return Arrays.asList(JakartaWebSocketShutdownContainer.class, JakartaWebSocketServletContainerInitializer.class,
				JerseyServletContainerInitializer.class, SpringServletContainerInitializer.class);
	}
}
