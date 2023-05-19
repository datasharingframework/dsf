package dev.dsf.fhir.websocket;

import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.socket.server.standard.ServerEndpointRegistration;

import jakarta.websocket.Endpoint;

public class ServerEndpointRegistrationForAuthentication extends ServerEndpointRegistration implements InitializingBean
{
	private final Configurator containerDefaultConfigurator;

	public ServerEndpointRegistrationForAuthentication(Configurator containerDefaultConfigurator, String path,
			Endpoint endpoint)
	{
		super(path, endpoint);

		this.containerDefaultConfigurator = containerDefaultConfigurator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(containerDefaultConfigurator, "containerDefaultConfigurator");
	}

	@Override
	public Configurator getContainerDefaultConfigurator()
	{
		return containerDefaultConfigurator;
	}
}
