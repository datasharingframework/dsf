package dev.dsf.fhir.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.fhir.validation.SnapshotGenerator;
import dev.dsf.fhir.validation.SnapshotGeneratorImpl;

@Configuration
public class SnapshotConfig
{
	@Autowired
	private FhirConfig fhirConfig;

	@Autowired
	private ValidationConfig validationConfig;

	@Bean
	public SnapshotGenerator snapshotGenerator()
	{
		return new SnapshotGeneratorImpl(fhirConfig.fhirContext(), validationConfig.validationSupport());
	}
}