package dev.dsf.bpe.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.json.ObjectMapperFactory;
import dev.dsf.fhir.variables.FhirResourceSerializer;
import dev.dsf.fhir.variables.FhirResourcesListSerializer;
import dev.dsf.fhir.variables.TargetSerializer;
import dev.dsf.fhir.variables.TargetsSerializer;

@Configuration
public class SerializerConfig
{
	@Autowired
	private FhirContext fhirContext;

	@Bean
	public ObjectMapper objectMapper()
	{
		return ObjectMapperFactory.createObjectMapper(fhirContext);
	}

	@Bean
	public FhirResourceSerializer fhirResourceSerializer()
	{
		return new FhirResourceSerializer(fhirContext);
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
