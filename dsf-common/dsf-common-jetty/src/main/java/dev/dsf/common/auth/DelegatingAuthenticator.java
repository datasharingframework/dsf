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

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Objects;

import org.eclipse.jetty.ee11.servlet.ServletContextRequest;
import org.eclipse.jetty.ee11.servlet.SessionHandler;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.security.AuthenticationState;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.Authenticator.Configuration.Wrapper;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.security.openid.OpenIdAuthenticator;
import org.eclipse.jetty.security.openid.OpenIdLoginService;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;

import jakarta.servlet.http.Cookie;

public class DelegatingAuthenticator extends LoginAuthenticator implements Authenticator
{
	private final SessionHandler sessionHandler;
	private final StatusPortAuthenticator statusPortAuthenticator;
	private final ClientCertificateAuthenticator clientCertificateAuthenticator;
	private final BearerTokenAuthenticator bearerTokenAuthenticator;
	private final OpenIdAuthenticator openIdAuthenticator;
	private final OpenIdLoginService openIdLoginService;

	private final BackChannelLogoutAuthenticator backChannelLogoutAuthenticator;

	public DelegatingAuthenticator(SessionHandler sessionHandler, StatusPortAuthenticator statusPortAuthenticator,
			ClientCertificateAuthenticator clientCertificateAuthenticator,
			BearerTokenAuthenticator bearerTokenAuthenticator, OpenIdAuthenticator openIdAuthenticator,
			OpenIdLoginService openIdLoginService, BackChannelLogoutAuthenticator backChannelLogoutAuthenticator)
	{
		Objects.requireNonNull(sessionHandler, "sessionHandler");
		this.sessionHandler = sessionHandler;
		Objects.requireNonNull(statusPortAuthenticator, "statusPortAuthenticator");
		this.statusPortAuthenticator = statusPortAuthenticator;

		Objects.requireNonNull(clientCertificateAuthenticator, "clientCertificateAuthenticator");
		this.clientCertificateAuthenticator = clientCertificateAuthenticator;

		// optional
		this.bearerTokenAuthenticator = bearerTokenAuthenticator;
		this.openIdAuthenticator = openIdAuthenticator;
		this.openIdLoginService = openIdLoginService;
		this.backChannelLogoutAuthenticator = backChannelLogoutAuthenticator;
	}

	@Override
	public void setConfiguration(Configuration configuration)
	{
		super.setConfiguration(configuration);

		clientCertificateAuthenticator.setConfiguration(configuration);

		if (bearerTokenAuthenticator != null)
			bearerTokenAuthenticator.setConfiguration(configuration);

		if (openIdAuthenticator != null)
		{
			Configuration openIdConfig = new Wrapper(configuration)
			{
				@Override
				public LoginService getLoginService()
				{
					return openIdLoginService;
				}
			};
			openIdAuthenticator.setConfiguration(openIdConfig);
		}
	}

	@Override
	public String getAuthenticationType()
	{
		return "DELEGATING_AUTHENTICATOR";
	}

	private boolean requestHasCertificate(Request request)
	{
		X509Certificate[] certificates = (X509Certificate[]) request
				.getAttribute("jakarta.servlet.request.X509Certificate");

		return certificates != null && certificates.length > 0;
	}

	private boolean isFrontendRequest(Request request)
	{
		if (request instanceof ServletContextRequest servletRequest)
		{
			if (servletRequest.getServletApiRequest() != null)
			{
				Cookie[] cookies = servletRequest.getServletApiRequest().getCookies();

				boolean sessionCookieSet = cookies != null && Arrays.stream(cookies)
						.anyMatch(c -> sessionHandler.getSessionCookie().equals(c.getName()) && c.getValue() != null);

				if (sessionCookieSet)
					return true;
			}
		}

		String accept = request.getHeaders().get(HttpHeader.ACCEPT);

		return accept != null && accept.contains(MimeTypes.Type.TEXT_HTML.asString());
	}

	@Override
	public Request prepareRequest(Request request, AuthenticationState authenticationState)
	{
		if (statusPortAuthenticator.isStatusPortRequest(request))
			statusPortAuthenticator.prepareRequest(request, authenticationState);
		else if (backChannelLogoutAuthenticator != null
				&& backChannelLogoutAuthenticator.isBackChannelLogoutRequest(request))
			backChannelLogoutAuthenticator.prepareRequest(request, authenticationState);
		else if (requestHasCertificate(request))
			clientCertificateAuthenticator.prepareRequest(request, authenticationState);
		else if (openIdAuthenticator != null && isFrontendRequest(request))
			openIdAuthenticator.prepareRequest(request, authenticationState);
		else if (bearerTokenAuthenticator != null)
			bearerTokenAuthenticator.prepareRequest(request, authenticationState);

		return request;
	}

	@Override
	public AuthenticationState validateRequest(Request request, Response response, Callback callback)
			throws ServerAuthException
	{
		if (statusPortAuthenticator.isStatusPortRequest(request))
			return statusPortAuthenticator.validateRequest(request, response, callback);
		else if (backChannelLogoutAuthenticator != null
				&& backChannelLogoutAuthenticator.isBackChannelLogoutRequest(request))
			return backChannelLogoutAuthenticator.validateRequest(request, response, callback);
		else if (requestHasCertificate(request))
			return clientCertificateAuthenticator.validateRequest(request, response, callback);
		else if (openIdAuthenticator != null && isFrontendRequest(request))
			return openIdAuthenticator.validateRequest(request, response, callback);
		else if (bearerTokenAuthenticator != null)
			return bearerTokenAuthenticator.validateRequest(request, response, callback);
		else
			return null;
	}

	@Override
	public void logout(Request request, Response response)
	{
		if (openIdAuthenticator != null && isFrontendRequest(request))
			openIdAuthenticator.logout(request, response);
		else
			super.logout(request, response);
	}
}
