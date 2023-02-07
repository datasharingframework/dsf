package dev.dsf.fhir.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.fhir.cors.CorsFilterConfig;
import dev.dsf.fhir.cors.CorsFilterConfigImpl;

@Configuration
public class CorsConfig
{
	@Autowired
	private PropertiesConfig propertiesConfig;

	@Bean
	public CorsFilterConfig corsFilterConfig()
	{
		return CorsFilterConfigImpl.createConfigForAllowedOrigins(propertiesConfig.getAllowedOrigins());
	}
}
