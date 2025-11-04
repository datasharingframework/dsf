/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.fhir;

import java.util.logging.Level;

import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.logging.LoggingFeature.Verbosity;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import dev.dsf.common.auth.filter.AuthenticationFilter;
import dev.dsf.common.auth.logging.CurrentUserLogger;
import dev.dsf.common.auth.logging.CurrentUserMdcLogger;
import dev.dsf.fhir.spring.config.PropertiesConfig;
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

		register(AuthenticationFilter.class);
		register(RolesAllowedDynamicFeature.class);

		if (context.getBean(PropertiesConfig.class).getDebugLogMessageCurrentUser())
			register(CurrentUserLogger.class);

		register(CurrentUserMdcLogger.class);

		if (context.getBean(PropertiesConfig.class).getDebugLogMessageWebserviceRequest())
		{
			java.util.logging.Logger l = java.util.logging.Logger.getLogger(FhirJerseyApplication.class.getName());
			l.setLevel(Level.FINE);
			LoggingFeature loggingFeature = new LoggingFeature(l, Verbosity.HEADERS_ONLY);
			register(loggingFeature);
		}
	}
}
