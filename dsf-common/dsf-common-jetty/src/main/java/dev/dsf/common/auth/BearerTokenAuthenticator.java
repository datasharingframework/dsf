package dev.dsf.common.auth;

import java.io.IOException;
import java.util.Objects;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Authentication.User;
import org.eclipse.jetty.server.UserIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class BearerTokenAuthenticator extends LoginAuthenticator
{
	private static final Logger logger = LoggerFactory.getLogger(BearerTokenAuthenticator.class);

	private final DsfOpenIdConfiguration openIdConfiguration;

	public BearerTokenAuthenticator(DsfOpenIdConfiguration openIdConfiguration)
	{
		Objects.requireNonNull(openIdConfiguration, "openIdConfiguration");
		this.openIdConfiguration = openIdConfiguration;
	}

	@Override
	public String getAuthMethod()
	{
		return "BEARER_TOKEN";
	}

	@Override
	public Authentication validateRequest(ServletRequest request, ServletResponse response, boolean mandatory)
			throws ServerAuthException
	{
		final HttpServletRequest servletRequest = (HttpServletRequest) request;
		final HttpServletResponse servletResponse = (HttpServletResponse) response;

		try
		{
			String authorizationHeader = servletRequest.getHeader(HttpHeader.AUTHORIZATION.asString());
			if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer "))
			{
				servletResponse.setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), "Bearer");
				servletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return Authentication.SEND_FAILURE;
			}

			Algorithm algorithm = Algorithm.RSA256(openIdConfiguration.getRsaKeyProvider());
			JWTVerifier verifier = JWT.require(algorithm).withIssuer(openIdConfiguration.getIssuer()).acceptLeeway(1)
					.build();

			String accessToken = authorizationHeader.substring(7, authorizationHeader.length());

			try
			{
				DecodedJWT jwt = verifier.verify(accessToken);
				if (!jwt.getClaims().containsKey("sub") && !jwt.getClaims().containsKey("sid"))
				{
					logger.warn("Access token has no sub and no sid claim");
					servletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
					return Authentication.SEND_FAILURE;
				}

				logger.debug("Access token claims: {}", jwt.getClaims());
				UserIdentity user = login(null, accessToken, request);
				if (user == null)
				{
					servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
					return Authentication.SEND_FAILURE;
				}

				return new UserAuthentication(getAuthMethod(), user);
			}
			catch(TokenExpiredException e)
			{
				servletResponse.setHeader(HttpHeader.WWW_AUTHENTICATE.asString(), "Bearer error=\"invalid_token\", error_description=\"The access token expired\"");
				servletResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return Authentication.SEND_FAILURE;
			}
			catch (JWTVerificationException e)
			{
				servletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
				return Authentication.SEND_FAILURE;
			}
		}
		catch (IOException e)
		{
			throw new ServerAuthException(e);
		}
	}

	@Override
	public boolean secureResponse(ServletRequest request, ServletResponse response, boolean mandatory,
			User validatedUser) throws ServerAuthException
	{
		return true; // nothing to do
	}
}
