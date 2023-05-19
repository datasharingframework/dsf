package dev.dsf.bpe.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.dsf.bpe.variables.FhirResourceSerializer;
import dev.dsf.bpe.variables.FhirResourcesListSerializer;
import dev.dsf.bpe.variables.ObjectMapperFactory;
import dev.dsf.bpe.variables.TargetSerializer;
import dev.dsf.bpe.variables.TargetsSerializer;

@Configuration
public class SerializerConfig
{
	@Autowired
	private FhirConfig fhirConfig;

	@Bean
	public ObjectMapper objectMapper()
	{
		return ObjectMapperFactory.createObjectMapper(fhirConfig.fhirContext());
	}

	@Bean
	public FhirResourceSerializer fhirResourceSerializer()
	{
		return new FhirResourceSerializer(fhirConfig.fhirContext());
	}

	@Bean
	public FhirResourcesListSerializer fhirResourcesListSerializer()
	{
		return new FhirResourcesListSerializer(objectMapper());
	}

	@Bean
	public TargetSerializer targetSerializer()
	{
		return new TargetSerializer(objectMapper());
	}

	@Bean
	public TargetsSerializer targetsSerializer()
	{
		return new TargetsSerializer(objectMapper());
	}
}
