package dev.dsf.bpe.config;

import java.util.Arrays;
import java.util.List;

import org.glassfish.jersey.servlet.init.JerseyServletContainerInitializer;
import org.springframework.web.SpringServletContainerInitializer;

import dev.dsf.common.config.AbstractHttpJettyConfig;
import jakarta.servlet.ServletContainerInitializer;

public class BpeHttpJettyConfig extends AbstractHttpJettyConfig
{
	@Override
	protected String mavenServerModuleName()
	{
		return "bpe-server";
	}

	@Override
	protected List<Class<? extends ServletContainerInitializer>> servletContainerInitializers()
	{
		return Arrays.asList(JerseyServletContainerInitializer.class, SpringServletContainerInitializer.class);
	}
}
