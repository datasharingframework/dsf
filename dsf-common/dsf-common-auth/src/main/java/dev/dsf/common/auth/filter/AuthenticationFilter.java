package dev.dsf.common.auth.filter;

import java.io.IOException;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;

public class AuthenticationFilter implements ContainerRequestFilter
{
	@Override
	public void filter(final ContainerRequestContext requestContext) throws IOException
	{
		if (requestContext.getSecurityContext().getUserPrincipal() == null)
			throw new ForbiddenException();
	}
}
