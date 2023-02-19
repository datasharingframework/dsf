package dev.dsf.common.jetty;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

public class SessionInvalidator implements ServletRequestListener
{
	@Override
	public void requestInitialized(ServletRequestEvent sre)
	{
		// nothing to do
	}

	@Override
	public void requestDestroyed(ServletRequestEvent sre)
	{
		HttpServletRequest servletRequest = (HttpServletRequest) sre.getServletRequest();
		HttpSession session = servletRequest.getSession(false);

		if (session != null)
			session.invalidate();
	}
}
