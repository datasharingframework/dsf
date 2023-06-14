package dev.dsf.common.auth;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Objects;

import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.LoginService;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.WrappedAuthConfiguration;
import org.eclipse.jetty.security.authentication.LoginAuthenticator;
import org.eclipse.jetty.security.openid.OpenIdAuthenticator;
import org.eclipse.jetty.security.openid.OpenIdLoginService;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Authentication.User;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.session.SessionHandler;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

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
	public void setConfiguration(AuthConfiguration configuration)
	{
		clientCertificateAuthenticator.setConfiguration(configuration);

		if (bearerTokenAuthenticator != null)
			bearerTokenAuthenticator.setConfiguration(configuration);

		if (openIdAuthenticator != null)
		{
			AuthConfiguration openIdConfig = new WrappedAuthConfiguration(configuration)
			{
				public LoginService getLoginService()
				{
					return openIdLoginService;
				}
			};
			openIdAuthenticator.setConfiguration(openIdConfig);
		}
	}

	@Override
	public String getAuthMethod()
	{
		return "DELEGATING_AUTHENTICATOR";
	}

	private boolean requestHasCertificate(ServletRequest request)
	{
		X509Certificate[] certificates = (X509Certificate[]) request
				.getAttribute("jakarta.servlet.request.X509Certificate");

		return certificates != null && certificates.length > 0;
	}

	private boolean isFrontendRequest(ServletRequest request)
	{
		final HttpServletRequest servletRequest = (HttpServletRequest) request;

		boolean sessionCookieSet = servletRequest.getCookies() != null && Arrays.stream(servletRequest.getCookies())
				.anyMatch(c -> sessionHandler.getSessionCookie().equals(c.getName()) && c.getValue() != null);

		if (sessionCookieSet)
			return true;

		return servletRequest.getHeader(HttpHeader.ACCEPT.asString()) != null
				&& servletRequest.getHeader(HttpHeader.ACCEPT.asString()).contains(MimeTypes.Type.TEXT_HTML.asString());
	}

	@Override
	public void prepareRequest(ServletRequest request)
	{
		if (statusPortAuthenticator.isStatusPortRequest(request))
			statusPortAuthenticator.prepareRequest(request);
		else if (backChannelLogoutAuthenticator != null
				&& backChannelLogoutAuthenticator.isBackChannelLogoutRequest(request))
			backChannelLogoutAuthenticator.prepareRequest(request);
		else if (requestHasCertificate(request))
			clientCertificateAuthenticator.prepareRequest(request);
		else if (openIdAuthenticator != null && isFrontendRequest(request))
			openIdAuthenticator.prepareRequest(request);
		else if (bearerTokenAuthenticator != null)
			bearerTokenAuthenticator.prepareRequest(request);
	}

	@Override
	public Authentication validateRequest(ServletRequest request, ServletResponse response, boolean mandatory)
			throws ServerAuthException
	{
		if (statusPortAuthenticator.isStatusPortRequest(request))
			return statusPortAuthenticator.validateRequest(request, response, mandatory);
		else if (backChannelLogoutAuthenticator != null
				&& backChannelLogoutAuthenticator.isBackChannelLogoutRequest(request))
			return backChannelLogoutAuthenticator.validateRequest(request, response, mandatory);
		else if (requestHasCertificate(request))
			return clientCertificateAuthenticator.validateRequest(request, response, mandatory);
		else if (openIdAuthenticator != null && isFrontendRequest(request))
			return openIdAuthenticator.validateRequest(request, response, mandatory);
		else if (bearerTokenAuthenticator != null)
			return bearerTokenAuthenticator.validateRequest(request, response, mandatory);
		else
			return Authentication.UNAUTHENTICATED;
	}

	@Override
	public boolean secureResponse(ServletRequest request, ServletResponse response, boolean mandatory,
			User validatedUser) throws ServerAuthException
	{
		if (statusPortAuthenticator.isStatusPortRequest(request))
			return statusPortAuthenticator.secureResponse(request, response, mandatory, validatedUser);
		else if (backChannelLogoutAuthenticator != null
				&& backChannelLogoutAuthenticator.isBackChannelLogoutRequest(request))
			return backChannelLogoutAuthenticator.secureResponse(request, response, mandatory, validatedUser);
		else if (requestHasCertificate(request))
			return clientCertificateAuthenticator.secureResponse(request, response, mandatory, validatedUser);
		else if (openIdAuthenticator != null && isFrontendRequest(request))
			return openIdAuthenticator.secureResponse(request, response, mandatory, validatedUser);
		else if (bearerTokenAuthenticator != null)
			return bearerTokenAuthenticator.secureResponse(request, response, mandatory, validatedUser);
		else
			return false;
	}

	@Override
	public void logout(ServletRequest request)
	{
		Request baseRequest = Request.getBaseRequest(request);

		if (openIdAuthenticator != null && openIdAuthenticator.getAuthMethod().equals(baseRequest.getAuthType()))
			openIdAuthenticator.logout(request);
		else
			super.logout(request);
	}
}
