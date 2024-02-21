package dev.dsf.bpe.config;

import java.util.List;

import org.glassfish.jersey.servlet.init.JerseyServletContainerInitializer;
import org.springframework.web.SpringServletContainerInitializer;

import dev.dsf.common.config.AbstractHttpsJettyConfig;
import jakarta.servlet.ServletContainerInitializer;

public class BpeHttpsJettyConfig extends AbstractHttpsJettyConfig
{
	@Override
	protected String mavenServerModuleName()
	{
		return "bpe-server";
	}

	@Override
	protected List<Class<? extends ServletContainerInitializer>> servletContainerInitializers()
	{
		return List.of(JerseyServletContainerInitializer.class, SpringServletContainerInitializer.class);
	}
}
