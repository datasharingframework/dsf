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
package dev.dsf.common.auth;

import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.UserIdentity;
import org.eclipse.jetty.security.openid.OpenIdCredentials;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Session;
import org.springframework.web.context.support.WebApplicationContextUtils;

import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.IdentityProvider;

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
		public boolean isUserInRole(String role)
		{
			return roles.contains(role);
		}
	}

	private final AtomicReference<IdentityProvider> identityProvider = new AtomicReference<>(null);

	private final WebAppContext webAppContext;

	public DsfLoginService(WebAppContext webAppContext)
	{
		this.webAppContext = Objects.requireNonNull(webAppContext, "webAppContext");
	}

	@Override
	public String getName()
	{
		return "DsfLoginService";
	}

	@Override
	public UserIdentity login(String username, Object credentials, Request request,
			Function<Boolean, Session> getOrCreateSession)
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
			ip = WebApplicationContextUtils.getWebApplicationContext(webAppContext.getServletContext())
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
