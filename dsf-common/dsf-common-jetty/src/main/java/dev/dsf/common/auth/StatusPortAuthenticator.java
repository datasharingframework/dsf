package dev.dsf.common.auth;

import java.util.Objects;
import java.util.function.Supplier;

import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.security.Authenticator;
import org.eclipse.jetty.security.ServerAuthException;
import org.eclipse.jetty.security.UserAuthentication;
import org.eclipse.jetty.server.Authentication;
import org.eclipse.jetty.server.Authentication.User;

import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

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
	public void setConfiguration(AuthConfiguration configuration)
	{
	}

	@Override
	public String getAuthMethod()
	{
		return "STATUS_PORT_AUTHENTICATOR";
	}

	public boolean isStatusPortRequest(ServletRequest req)
	{
		HttpServletRequest request = (HttpServletRequest) req;
		return HttpMethod.GET.is(request.getMethod()) && STATUS_PATH.equals(request.getPathInfo())
				&& statusPortSupplier.get() != null && statusPortSupplier.get() == request.getLocalPort();
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
		if (isStatusPortRequest(request))
			return new UserAuthentication(getAuthMethod(), null);
		else
			return Authentication.UNAUTHENTICATED;
	}

	@Override
	public boolean secureResponse(ServletRequest request, ServletResponse response, boolean mandatory,
			User validatedUser) throws ServerAuthException
	{
		return true; // nothing to do
	}
}
