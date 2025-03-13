package dev.dsf.fhir.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;

@Configuration
public class HelperConfig
{
	@Autowired
	private PropertiesConfig propertiesConfig;

	@Bean
	public ExceptionHandler exceptionHandler()
	{
		return new ExceptionHandler(responseGenerator());
	}

	@Bean
	public ResponseGenerator responseGenerator()
	{
		return new ResponseGenerator(propertiesConfig.getDsfServerBaseUrl());
	}

	@Bean
	public ParameterConverter parameterConverter()
	{
		return new ParameterConverter(exceptionHandler());
	}
}
