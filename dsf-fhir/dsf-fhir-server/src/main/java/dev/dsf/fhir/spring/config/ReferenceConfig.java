package dev.dsf.fhir.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceCleanerImpl;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceExtractorImpl;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.service.ReferenceResolverImpl;

@Configuration
public class ReferenceConfig
{
	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private HelperConfig helperConfig;

	@Autowired
	private DaoConfig daoConfig;

	@Autowired
	private ClientConfig clientConfig;

	@Bean
	public ReferenceExtractor referenceExtractor()
	{
		return new ReferenceExtractorImpl();
	}

	@Bean
	public ReferenceResolver referenceResolver()
	{
		return new ReferenceResolverImpl(propertiesConfig.getServerBaseUrl(), daoConfig.daoProvider(),
				helperConfig.responseGenerator(), helperConfig.exceptionHandler(), clientConfig.clientProvider(),
				helperConfig.parameterConverter());
	}

	@Bean
	public ReferenceCleaner referenceCleaner()
	{
		return new ReferenceCleanerImpl(referenceExtractor());
	}
}
