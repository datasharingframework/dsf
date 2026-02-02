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

import java.util.Objects;

import org.eclipse.jetty.ee11.servlet.security.ConstraintSecurityHandler;
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
