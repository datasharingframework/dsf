package dev.dsf.common.auth.logging;

import java.io.IOException;
import java.security.Principal;

import org.eclipse.jetty.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.common.auth.conf.PractitionerIdentity;
import jakarta.ws.rs.ConstrainedTo;
import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;

@ConstrainedTo(RuntimeType.SERVER)
@PreMatching
public abstract class AbstractUserLogger implements ContainerRequestFilter, ContainerResponseFilter
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractUserLogger.class);

	@Override
	public final void filter(ContainerRequestContext requestContext) throws IOException
	{
		Principal principal = requestContext.getSecurityContext().getUserPrincipal();

		if (principal instanceof OrganizationIdentity organization)
			before(organization);
		else if (principal instanceof PractitionerIdentity practitioner)
			before(practitioner);
		else if (principal instanceof UserPrincipal userPrincipal)
			before(userPrincipal);
		else
			logger.warn("Unknown current user principal of type {}", principal.getClass().getName());
	}

	protected abstract void before(OrganizationIdentity organization);

	protected abstract void before(PractitionerIdentity practitioner);

	protected abstract void before(UserPrincipal userPrincipal);

	@Override
	public final void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
			throws IOException
	{
		after();
	}

	protected void after()
	{
	}
}
