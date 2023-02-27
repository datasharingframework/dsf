package dev.dsf.fhir.websocket;

import javax.servlet.http.HttpSession;
import javax.websocket.Endpoint;
import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

import org.springframework.web.socket.server.standard.ServerEndpointRegistration;

import dev.dsf.fhir.authentication.AuthenticationFilter;
import dev.dsf.fhir.authentication.User;

public class ServerEndpointRegistrationForAuthentication extends ServerEndpointRegistration
{
	public ServerEndpointRegistrationForAuthentication(String path, Endpoint endpoint)
	{
		super(path, endpoint);
	}

	@Override
	public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response)
	{
		HttpSession httpSession = (HttpSession) request.getHttpSession();
		User user = (User) httpSession.getAttribute(AuthenticationFilter.USER_PROPERTY);

		// don't use ServerEndpointRegistration#getUserProperties()
		sec.getUserProperties().put(ServerEndpoint.USER_PROPERTY, user);
	}
}
