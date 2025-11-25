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
import java.util.function.Supplier;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.security.AuthenticationState;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserPrincipal;
import org.eclipse.jetty.security.authentication.LoginAuthenticator.UserAuthenticationSucceeded;
import org.eclipse.jetty.security.internal.DefaultUserIdentity;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

public class StatusPortAuthenticator implements Authenticator
{
	private static final String STATUS_PATH = "/status";

	private final Supplier<Integer> statusPortSupplier;

	public StatusPortAuthenticator(Supplier<Integer> statusPortSupplier)
	{
		Objects.requireNonNull(statusPortSupplier, "statusPortSupplier");

		this.statusPortSupplier = statusPortSupplier;
	}

	@Override
	public void setConfiguration(Configuration configuration)
	{
	}

	@Override
	public String getAuthenticationType()
	{
		return "STATUS_PORT_AUTHENTICATOR";
	}

	public boolean isStatusPortRequest(Request req)
	{
		return statusPortSupplier.get() != null && statusPortSupplier.get() == req.getHttpURI().getPort();
	}

	private boolean isStatusPortAndPathGetRequest(Request req)
	{
		return HttpMethod.GET.is(req.getMethod()) && STATUS_PATH.equals(Request.getPathInContext(req))
				&& isStatusPortRequest(req);
	}

	@Override
	public AuthenticationState validateRequest(Request request, Response response, Callback callback)
			throws ServerAuthException
	{
		if (isStatusPortAndPathGetRequest(request))
			return new UserAuthenticationSucceeded(getAuthenticationType(), new DefaultUserIdentity(null,
					new UserPrincipal("STATUS_PORT_USER", null), new String[] { "STATUS_PORT_ROLE" }));
		else
			return null;
	}
}
