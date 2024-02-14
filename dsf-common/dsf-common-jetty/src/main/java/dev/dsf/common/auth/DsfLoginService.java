package dev.dsf.common.auth;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.openid.OpenIdCredentials;
import org.eclipse.jetty.server.UserIdentity;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.springframework.web.context.support.WebApplicationContextUtils;

import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.IdentityProvider;
import jakarta.servlet.ServletRequest;

public class DsfLoginService implements LoginService
{
	private static final class UserIdentityImpl implements UserIdentity
	{
		private final Principal principal;
		private final Set<String> roles = new HashSet<>();

		private UserIdentityImpl(Principal principal, Set<String> roles)
		{
			this.principal = principal;

			if (roles != null)
				this.roles.addAll(roles);
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
			return roles.contains(role);
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

		Identity identity = null;
		if (credentials instanceof X509Certificate[] c)
			identity = identityProvider.getIdentity(c);
		else if (credentials instanceof OpenIdCredentials o)
			identity = identityProvider.getIdentity(new DsfOpenIdCredentialsImpl(o));
		else if (credentials instanceof String s)
			identity = identityProvider.getIdentity(new DsfOpenIdCredentialsImpl(s));

		if (identity == null)
			return null;

		return new UserIdentityImpl(identity, toRoles(identity));
	}

	private Set<String> toRoles(Identity identity)
	{
		return identity.getDsfRoles().stream().map(DsfRole::name).collect(Collectors.toSet());
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
