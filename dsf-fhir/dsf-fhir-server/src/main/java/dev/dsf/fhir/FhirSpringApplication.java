package dev.dsf.fhir;

import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

public class FhirSpringApplication implements WebApplicationInitializer
{
	@Override
	public void onStartup(ServletContext servletContext) throws ServletException
	{
		AnnotationConfigWebApplicationContext context = getContext();
		servletContext.addListener(new FhirContextLoaderListener(context));
		servletContext.addListener(new RequestContextListener());
	}

	private AnnotationConfigWebApplicationContext getContext()
	{
		AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
		context.setConfigLocation("dev.dsf.fhir.spring.config");

		return context;
	}
}
