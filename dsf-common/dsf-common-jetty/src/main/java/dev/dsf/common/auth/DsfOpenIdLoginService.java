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

import java.util.function.Function;

import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.UserIdentity;
import org.eclipse.jetty.security.openid.OpenIdConfiguration;
import org.eclipse.jetty.security.openid.OpenIdCredentials;
import org.eclipse.jetty.security.openid.OpenIdLoginService;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.PractitionerIdentity;

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
	public UserIdentity login(String identifier, Object credentials, Request request,
			Function<Boolean, Session> getOrCreateSession)
	{
		OpenIdCredentials openIdCredentials = (OpenIdCredentials) credentials;
		try
		{
			openIdCredentials.redeemAuthCode(configuration);
		}
		catch (Exception e)
		{
			logger.debug("Unable to redeem auth code", e);
			logger.warn("Unable to redeem auth code: {} - {}", e.getClass().getName(), e.getMessage());

			return null;
		}

		return loginService.login(openIdCredentials.getUserId(), credentials, request, getOrCreateSession);
	}

	@Override
	public boolean validate(UserIdentity user)
	{
		if (!(user.getUserPrincipal() instanceof PractitionerIdentity identity))
			return false;

		if (identity.getCredentials().isEmpty())
		{
			logger.warn("No credentials");
			return false;
		}

		long expiry = identity.getCredentials().get().getLongClaim("exp");
		long currentTimeSeconds = (long) (System.currentTimeMillis() / 1000F);
		if (currentTimeSeconds > expiry)
		{
			logger.debug("ID Token has expired");
			return false;
		}

		return loginService == null || loginService.validate(user);
	}
}
