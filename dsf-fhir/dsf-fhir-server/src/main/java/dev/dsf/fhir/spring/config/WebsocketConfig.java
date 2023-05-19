package dev.dsf.fhir.spring.config;

import org.eclipse.jetty.websocket.jakarta.server.config.ContainerDefaultConfigurator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.socket.server.standard.ServerEndpointRegistration;

import dev.dsf.fhir.websocket.ServerEndpoint;
import dev.dsf.fhir.websocket.ServerEndpointRegistrationForAuthentication;
import jakarta.websocket.server.ServerEndpointConfig.Configurator;

@Configuration
public class WebsocketConfig
{
	@Autowired
	private EventConfig eventConfig;

	@Bean
	public ServerEndpoint subscriptionEndpoint()
	{
		return new ServerEndpoint(eventConfig.webSocketSubscriptionManager());
	}

	@Bean
	public ServerEndpointRegistration subscriptionEndpointRegistration()
	{
		return new ServerEndpointRegistrationForAuthentication(containerDefaultConfigurator(), ServerEndpoint.PATH,
				subscriptionEndpoint());
	}

	@Bean
	public ServerEndpointExporter endpointExporter()
	{
		return new ServerEndpointExporter();
	}

	@Bean
	public Configurator containerDefaultConfigurator()
	{
		return new ContainerDefaultConfigurator();
	}
}
