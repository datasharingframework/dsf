package dev.dsf.common.auth;

import java.io.IOException;
import java.util.Objects;

import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.RoleInfo;
import org.eclipse.jetty.security.SecurityHandler;
import org.eclipse.jetty.security.openid.OpenIdConfiguration;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.UserIdentity;

public class DsfSecurityHandler extends SecurityHandler
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

	// 1.
	@Override
	protected RoleInfo prepareConstraintInfo(String pathInContext, Request request)
	{
		return null; // no constraints
	}

	// 2.
	@Override
	protected boolean checkUserDataPermissions(String pathInContext, Request request, Response response,
			RoleInfo constraintInfo) throws IOException
	{
		return true; // nothing to check
	}

	// 3.
	@Override
	protected boolean isAuthMandatory(Request baseRequest, Response baseResponse, Object constraintInfo)
	{
		return true; // authentication mandatory
	}

	// 4. authenticator.validateRequest

	// 5.
	@Override
	protected boolean checkWebResourcePermissions(String pathInContext, Request request, Response response,
			Object constraintInfo, UserIdentity userIdentity) throws IOException
	{
		return true; // nothing to check
	}
}
