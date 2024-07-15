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

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

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
				Response.writeError(request, response, callback, HttpStatus.BAD_REQUEST_400);
				return AuthenticationState.SEND_FAILURE;
			}

			logger.debug("Access token claims: {}", jwt.getClaims());
			UserIdentity user = login(null, accessToken, request, response);
			if (user == null)
			{
				Response.writeError(request, response, callback, HttpStatus.FORBIDDEN_403);
				return AuthenticationState.SEND_FAILURE;
			}

			return new UserAuthenticationSucceeded(getAuthenticationType(), user);
		}
		catch (TokenExpiredException e)
		{
			response.getHeaders().put(HttpHeader.WWW_AUTHENTICATE.asString(),
					"Bearer error=\"invalid_token\", error_description=\"The access token expired\"");
			Response.writeError(request, response, callback, HttpStatus.UNAUTHORIZED_401);
			return AuthenticationState.SEND_FAILURE;
		}
		catch (JWTVerificationException e)
		{
			Response.writeError(request, response, callback, HttpStatus.BAD_REQUEST_400);
			return AuthenticationState.SEND_FAILURE;
		}
	}
}
