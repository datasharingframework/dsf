package dev.dsf.common.auth;

import java.util.Objects;

import org.eclipse.jetty.ee10.servlet.security.ConstraintSecurityHandler;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.Constraint;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.openid.OpenIdConfiguration;
import org.eclipse.jetty.server.Request;

public class DsfSecurityHandler extends ConstraintSecurityHandler
{
	public DsfSecurityHandler(LoginService loginService, Authenticator authenticator,
			OpenIdConfiguration openIdConfiguration)
	{
		setIdentityService(new DefaultIdentityService());

		Objects.requireNonNull(loginService, "loginService");
		setLoginService(loginService);

		Objects.requireNonNull(authenticator, "authenticator");
		setAuthenticator(authenticator);

		if (openIdConfiguration != null)
			addBean(openIdConfiguration);
	}

	@Override
	protected Constraint getConstraint(String pathInContext, Request request)
	{
		return Constraint.ANY_USER;
	}
}
