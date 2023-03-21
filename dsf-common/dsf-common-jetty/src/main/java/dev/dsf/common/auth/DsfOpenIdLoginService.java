package dev.dsf.common.auth;

import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.openid.OpenIdConfiguration;
import org.eclipse.jetty.security.openid.OpenIdCredentials;
import org.eclipse.jetty.security.openid.OpenIdLoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.PractitionerIdentity;
import jakarta.servlet.ServletRequest;

public class DsfOpenIdLoginService extends OpenIdLoginService
{
	private static final Logger logger = LoggerFactory.getLogger(DsfOpenIdLoginService.class);

	private final OpenIdConfiguration configuration;
	private final LoginService loginService;

	public DsfOpenIdLoginService(OpenIdConfiguration configuration, LoginService loginService)
	{
		super(configuration, loginService);

		this.configuration = configuration;
		this.loginService = loginService;
	}

	@Override
	public UserIdentity login(String identifier, Object credentials, ServletRequest req)
	{
		OpenIdCredentials openIdCredentials = (OpenIdCredentials) credentials;
		try
		{
			openIdCredentials.redeemAuthCode(configuration);
		}
		catch (Throwable e)
		{
			logger.warn("Unable to redeem auth code", e);
			return null;
		}

		return loginService.login(openIdCredentials.getUserId(), (OpenIdCredentials) credentials, req);
	}

	@Override
	public boolean validate(UserIdentity user)
	{
		if (!(user.getUserPrincipal() instanceof PractitionerIdentity))
			return false;

		PractitionerIdentity identity = (PractitionerIdentity) user.getUserPrincipal();

		if (identity.getCredentials().isEmpty())
		{
			logger.warn("No credentials");
			return false;
		}

		long expiry = (Long) identity.getCredentials().get().getLongClaim("exp");
		long currentTimeSeconds = (long) (System.currentTimeMillis() / 1000F);
		if (currentTimeSeconds > expiry)
		{
			logger.debug("ID Token has expired");
			return false;
		}

		return loginService == null || loginService.validate(user);
	}
}
