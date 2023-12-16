package dev.dsf.common.auth;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import javax.security.auth.Subject;

import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.openid.OpenIdCredentials;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.springframework.web.context.support.WebApplicationContextUtils;

import dev.dsf.common.auth.conf.IdentityProvider;
import jakarta.servlet.ServletRequest;

public class DsfLoginService implements LoginService
{
	private static final class UserIdentityImpl implements UserIdentity
	{
		private final Principal principal;

		private UserIdentityImpl(Principal principal)
		{
			this.principal = principal;
		}

		@Override
		public Subject getSubject()
		{
			return null;
		}

		@Override
		public Principal getUserPrincipal()
		{
			return principal;
		}

		@Override
		public boolean isUserInRole(String role, Scope scope)
		{
			return false;
		}
	}

	private final AtomicReference<IdentityProvider> identityProvider = new AtomicReference<>(null);
	private final ContextHandler contextHandler;

	public DsfLoginService(ContextHandler contextHandler)
	{
		this.contextHandler = Objects.requireNonNull(contextHandler, "contextHandler");
	}

	@Override
	public String getName()
	{
		return "DsfLoginService";
	}

	@Override
	public UserIdentity login(String username, Object credentials, ServletRequest request)
	{
		if (credentials == null)
			return null;

		IdentityProvider identityProvider = getIdentityProvider();
		if (identityProvider == null)
			return null;

		Principal principal = null;
		if (credentials instanceof X509Certificate[] c)
			principal = identityProvider.getIdentity(c);
		else if (credentials instanceof OpenIdCredentials o)
			principal = identityProvider.getIdentity(new DsfOpenIdCredentialsImpl(o));
		else if (credentials instanceof String s)
			principal = identityProvider.getIdentity(new DsfOpenIdCredentialsImpl(s));

		if (principal == null)
			return null;

		return new UserIdentityImpl(principal);
	}

	protected IdentityProvider getIdentityProvider()
	{
		IdentityProvider ip = identityProvider.get();
		if (ip == null)
		{
			ip = WebApplicationContextUtils.getWebApplicationContext(contextHandler.getServletContext())
					.getBean(IdentityProvider.class);
			if (identityProvider.compareAndSet(null, ip))
				return ip;
			else
				return identityProvider.get();
		}
		else
			return ip;
	}

	@Override
	public boolean validate(UserIdentity user)
	{
		return true;
	}

	@Override
	public IdentityService getIdentityService()
	{
		return null;
	}

	@Override
	public void setIdentityService(IdentityService identityService)
	{
	}

	@Override
	public void logout(UserIdentity user)
	{
	}
}
