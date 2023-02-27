package dev.dsf.fhir.spring.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.fhir.authentication.AuthenticationFilterConfig;
import dev.dsf.fhir.authentication.AuthenticationFilterConfigImpl;
import dev.dsf.fhir.authentication.DoesNotNeedAuthentication;

@Configuration
public class AuthenticationConfig
{
	@Autowired
	private List<DoesNotNeedAuthentication> doesNotNeedAuthentication;

	@Bean
	public AuthenticationFilterConfig authenticationFilterConfig()
	{
		return AuthenticationFilterConfigImpl.createConfigForPathsRequiringAuthentication(doesNotNeedAuthentication);
	}
}
