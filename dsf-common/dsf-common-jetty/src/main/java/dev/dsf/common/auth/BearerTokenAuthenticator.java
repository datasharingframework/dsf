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

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.security.AuthenticationState;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserIdentity;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;

import dev.dsf.common.oidc.JwtVerifier;
import dev.dsf.common.oidc.OidcClientException;

public class BearerTokenAuthenticator extends LoginAuthenticator
{
	private static final Logger logger = LoggerFactory.getLogger(BearerTokenAuthenticator.class);

	private final JwtVerifier jwtVerifier;

	public BearerTokenAuthenticator(JwtVerifier jwtVerifier)
	{
		this.jwtVerifier = Objects.requireNonNull(jwtVerifier, "jwtVerifier");
	}

	@Override
	public String getAuthenticationType()
	{
		return "BEARER_TOKEN";
	}

	@Override
	public AuthenticationState validateRequest(Request request, Response response, Callback callback)
			throws ServerAuthException
	{
		String authorizationHeader = request.getHeaders().get(HttpHeader.AUTHORIZATION);
		if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer "))
		{
			if (response.isCommitted())
				return null;

			response.getHeaders().put(HttpHeader.WWW_AUTHENTICATE.asString(), "Bearer");
			Response.writeError(request, response, callback, HttpStatus.UNAUTHORIZED_401);
			return AuthenticationState.SEND_FAILURE;
		}

		try
		{
			String token = authorizationHeader.substring(7, authorizationHeader.length());
			DecodedJWT jwt = jwtVerifier.verifyBearerToken(token);

			if (!jwt.getClaims().containsKey("sub") && !jwt.getClaims().containsKey("sid"))
			{
				logger.warn("Access token has no sub and no sid claim");
				Response.writeError(request, response, callback, HttpStatus.BAD_REQUEST_400);
				return AuthenticationState.SEND_FAILURE;
			}

			logger.debug("Access token claims: {}", jwt.getClaims());
			UserIdentity user = login(null, token, request, response);
			if (user == null)
			{
				Response.writeError(request, response, callback, HttpStatus.FORBIDDEN_403);
				return AuthenticationState.SEND_FAILURE;
			}

			return new UserAuthenticationSucceeded(getAuthenticationType(), user);
		}
		catch (TokenExpiredException e)
		{
			logger.debug("Bearer token expired, sending 401", e);
			logger.info("Bearer token expired, sending 401");

			response.getHeaders().put(HttpHeader.WWW_AUTHENTICATE.asString(),
					"Bearer error=\"invalid_token\", error_description=\"The access token expired\"");
			Response.writeError(request, response, callback, HttpStatus.UNAUTHORIZED_401);
			return AuthenticationState.SEND_FAILURE;
		}
		catch (JWTVerificationException | OidcClientException e)
		{
			logger.debug("Bearer token authorization failed, sending 400", e);
			logger.warn("Bearer token authorization failed, sending 400: {} - {}", e.getClass().getName(),
					e.getMessage());

			Response.writeError(request, response, callback, HttpStatus.BAD_REQUEST_400);
			return AuthenticationState.SEND_FAILURE;
		}
	}
}
