package dev.dsf.fhir;

import org.glassfish.jersey.server.ResourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import jakarta.inject.Inject;
import jakarta.servlet.ServletContext;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ext.Provider;

@ApplicationPath("/")
public final class FhirJerseyApplication extends ResourceConfig
{
	private static final Logger logger = LoggerFactory.getLogger(FhirJerseyApplication.class);

	@Inject
	public FhirJerseyApplication(ServletContext servletContext)
	{
		WebApplicationContext context = WebApplicationContextUtils.getWebApplicationContext(servletContext);

		context.getBeansWithAnnotation(Path.class).forEach((n, b) ->
		{
			logger.debug("Registering bean '{}' as singleton resource with path '{}'", n,
					servletContext.getContextPath() + "/" + b.getClass().getAnnotation(Path.class).value());

			register(b);
		});

		context.getBeansWithAnnotation(Provider.class).forEach((n, b) ->
		{
			logger.debug("Registering bean '{}' as singleton provider", n);

			register(b);
		});
	}
}
