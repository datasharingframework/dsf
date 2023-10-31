package dev.dsf.fhir.webservice.filter;

import java.io.IOException;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

@Provider
public class BrowserPolicyHeaderResponseFilter implements ContainerResponseFilter
{
	@Override
	public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException
	{
		if ((requestContext.getAcceptableMediaTypes() != null
				&& requestContext.getAcceptableMediaTypes().contains(MediaType.TEXT_HTML_TYPE))
				|| (requestContext.getUriInfo() != null && requestContext.getUriInfo().getPath() != null
						&& requestContext.getUriInfo().getPath().startsWith("static/")))
		{
			MultivaluedMap<String, Object> headers = responseContext.getHeaders();

			headers.add("X-Content-Type-Options", "nosniff");
			headers.add("Referrer-Policy", "strict-origin-when-cross-origin");
			headers.add("Cross-Origin-Opener-Policy", "same-origin");
			headers.add("Cross-Origin-Embedder-Policy", "require-corp");
			headers.add("Cross-Origin-Resource-Policy", "same-site");
			headers.add("Permissions-Policy", "geolocation=(), camera=(), microphone=()");

			if (requestContext.getUriInfo() != null && requestContext.getUriInfo().getPath() != null
					&& requestContext.getUriInfo().getPath().startsWith("Binary/"))
				headers.add("Content-Security-Policy",
						"base-uri 'self'; frame-ancestors 'none'; form-action 'self'; default-src 'none'; connect-src 'self'; img-src 'self';"
								+ " script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'");
			else
				headers.add("Content-Security-Policy",
						"base-uri 'self'; frame-ancestors 'none'; form-action 'self'; default-src 'none'; connect-src 'self'; img-src 'self';"
								+ " script-src 'self'; style-src 'self'");
		}
	}
}
