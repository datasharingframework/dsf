package dev.dsf.common.auth;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.openid.OpenIdAuthenticator;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Authentication.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

public class BackChannelLogoutAuthenticator implements Authenticator, HttpSessionListener, HttpSessionAttributeListener
{
	private static final Logger logger = LoggerFactory.getLogger(BackChannelLogoutAuthenticator.class);

	private final DsfOpenIdConfiguration openIdConfiguration;
	private final String ssoLogoutPath;

	private final ConcurrentMap<String, HttpSession> sessionsBySub = new ConcurrentHashMap<>();
	private final ConcurrentMap<String, HttpSession> sessionsBySid = new ConcurrentHashMap<>();

	public BackChannelLogoutAuthenticator(DsfOpenIdConfiguration openIdConfiguration, String ssoLogoutPath)
	{
		Objects.requireNonNull(openIdConfiguration, "openIdConfiguration");
		this.openIdConfiguration = openIdConfiguration;

		Objects.requireNonNull(ssoLogoutPath, "ssoLogoutPath");
		if (!ssoLogoutPath.startsWith("/"))
			this.ssoLogoutPath = "/" + ssoLogoutPath;
		else
			this.ssoLogoutPath = ssoLogoutPath;
	}

	@Override
	public void setConfiguration(AuthConfiguration configuration)
	{
	}

	@Override
	public String getAuthMethod()
	{
		return "BACK_CHANNEL_LOGOUT_AUTHENTICATOR";
	}

	public boolean isSsoLogout(ServletRequest req)
	{
		if (!openIdConfiguration.isBackChannelLogoutEnabled())
		{
			logger.debug("Back-Channel logout disabled");
			return false;
		}

		HttpServletRequest request = (HttpServletRequest) req;
		return HttpMethod.POST.is(request.getMethod()) && ssoLogoutPath.equals(request.getPathInfo())
				&& MimeTypes.Type.FORM_ENCODED.is(request.getContentType());
	}

	@Override
	public void prepareRequest(ServletRequest request)
	{
		// nothing to do
	}

	@Override
	public Authentication validateRequest(ServletRequest request, ServletResponse response, boolean mandatory)
			throws ServerAuthException
	{
		final HttpServletResponse servletResponse = (HttpServletResponse) response;

		try
		{
			String[] logoutTokens = request.getParameterValues("logout_token");
			if (logoutTokens == null || logoutTokens.length != 1)
			{
				servletResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
				return Authentication.SEND_FAILURE;
			}

			Algorithm algorithm = Algorithm.RSA256(openIdConfiguration.getRsaKeyProvider());
			JWTVerifier verifier = JWT.require(algorithm).withIssuer(openIdConfiguration.getIssuer())
					.withAudience(openIdConfiguration.getClientId()).acceptLeeway(1).withClaim("events", (claim,
							jwt) -> claim.asMap().containsKey("http://schemas.openid.net/event/backchannel-logout"))
					.build();

			try
			{
				DecodedJWT jwt = verifier.verify(logoutTokens[0]);
				if (!jwt.getClaims().containsKey("sub") && !jwt.getClaims().containsKey("sid"))
				{
					logger.warn("Logout Token has no sub and no sid claim");
					servletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
					return Authentication.SEND_FAILURE;
				}

				logger.debug("logout token claims: {}", jwt.getClaims());

				String sub = jwt.getClaim("sub").asString();
				String sid = jwt.getClaim("sid").asString();

				logger.debug("Invalidating session for sub/sid {}/{}", sub, sid);

				HttpSession sessionBySub = sessionsBySub.get(sub);
				if (sessionBySub != null)
					sessionBySub.invalidate();

				// session will have been removed if found by sub and invalidated
				HttpSession sessionBySid = sessionsBySid.get(sid);
				if (sessionBySid != null)
					sessionBySid.invalidate();

				return Authentication.SEND_SUCCESS;
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
	public void sessionCreated(HttpSessionEvent event)
	{
		if (openIdConfiguration.isBackChannelLogoutEnabled())
		{
			logger.debug("Session created, id: {}", event.getSession().getId());
			logger.debug("Session created, claims: {}", event.getSession().getAttribute(OpenIdAuthenticator.CLAIMS));

			Object claimsAttribute = event.getSession().getAttribute(OpenIdAuthenticator.CLAIMS);
			if (claimsAttribute != null)
			{
				@SuppressWarnings("unchecked")
				Map<String, Object> claims = (Map<String, Object>) claimsAttribute;

				String sub = (String) claims.get("sub");
				if (sub != null)
					sessionsBySub.put(sub, event.getSession());

				String sid = (String) claims.get("sid");
				if (sid != null)
					sessionsBySid.put(sid, event.getSession());
			}
		}
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event)
	{
		if (openIdConfiguration.isBackChannelLogoutEnabled())
		{
			logger.debug("Session destroyed, id: {}", event.getSession().getId());
			logger.debug("Session destroyed, claims: {}", event.getSession().getAttribute(OpenIdAuthenticator.CLAIMS));

			Object claimsAttribute = event.getSession().getAttribute(OpenIdAuthenticator.CLAIMS);
			if (claimsAttribute != null)
			{
				@SuppressWarnings("unchecked")
				Map<String, Object> claims = (Map<String, Object>) claimsAttribute;

				String sub = (String) claims.get("sub");
				if (sub != null)
					sessionsBySub.remove(sub, event.getSession());

				String sid = (String) claims.get("sid");
				if (sid != null)
					sessionsBySid.remove(sid, event.getSession());
			}
		}
	}

	@Override
	public void attributeAdded(HttpSessionBindingEvent event)
	{
		if (openIdConfiguration.isBackChannelLogoutEnabled() && OpenIdAuthenticator.CLAIMS.equals(event.getName()))
		{
			logger.debug("Attribute added, Session id: {}", event.getSession().getId());
			logger.debug("Attribute added, claims: {}", event.getValue());

			@SuppressWarnings("unchecked")
			Map<String, Object> claims = (Map<String, Object>) event.getValue();

			String sub = (String) claims.get("sub");
			if (sub != null)
				sessionsBySub.put(sub, event.getSession());

			String sid = (String) claims.get("sid");
			if (sid != null)
				sessionsBySid.put(sid, event.getSession());
		}
	}

	@Override
	public void attributeRemoved(HttpSessionBindingEvent event)
	{
		if (openIdConfiguration.isBackChannelLogoutEnabled() && OpenIdAuthenticator.CLAIMS.equals(event.getName()))
		{
			logger.debug("Attribute removed, Session id: {}", event.getSession().getId());
			logger.debug("Attribute removed, claims: {}", event.getValue());

			@SuppressWarnings("unchecked")
			Map<String, Object> claims = (Map<String, Object>) event.getValue();

			String sub = (String) claims.get("sub");
			if (sub != null)
				sessionsBySub.remove(sub, event.getSession());

			String sid = (String) claims.get("sid");
			if (sid != null)
				sessionsBySid.remove(sid, event.getSession());
		}
	}

	@Override
	public void attributeReplaced(HttpSessionBindingEvent event)
	{
		if (openIdConfiguration.isBackChannelLogoutEnabled() && OpenIdAuthenticator.CLAIMS.equals(event.getName()))
		{
			logger.debug("Attribute replaced, Session id: {}", event.getSession().getId());
			logger.debug("Attribute replaced, claims: {}", event.getValue());

			@SuppressWarnings("unchecked")
			Map<String, Object> claims = (Map<String, Object>) event.getValue();

			String sub = (String) claims.get("sub");
			if (sub != null)
				sessionsBySub.put(sub, event.getSession());

			String sid = (String) claims.get("sid");
			if (sid != null)
				sessionsBySid.put(sid, event.getSession());
		}
	}

	@Override
	public boolean secureResponse(ServletRequest request, ServletResponse response, boolean mandatory,
			User validatedUser) throws ServerAuthException
	{
		return request.isSecure();
	}
}
