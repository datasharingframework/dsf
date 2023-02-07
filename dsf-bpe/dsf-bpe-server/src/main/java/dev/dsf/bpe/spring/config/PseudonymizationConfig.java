package dev.dsf.bpe.spring.config;

import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.pseudonymization.client.PseudonymizationClientFactory;
import dev.dsf.pseudonymization.client.PseudonymizationClientServiceLoader;

@Configuration
public class PseudonymizationConfig
{
	private static final Logger logger = LoggerFactory.getLogger(PseudonymizationConfig.class);

	@Autowired
	private PropertiesConfig propertiesConfig;

	@Bean
	public PseudonymizationClientServiceLoader pseudonymizationClientServiceLoader()
	{
		return new PseudonymizationClientServiceLoader();
	}

	@Bean
	public PseudonymizationClientFactory pseudonymizationClientFactory()
	{
		PseudonymizationClientFactory factory = pseudonymizationClientServiceLoader()
				.getPseudonymizationClientFactory(propertiesConfig.getPseudonymizationClientFactoryClass())
				.orElseThrow(() -> new NoSuchElementException("Pseudonymization client factory with classname='"
						+ propertiesConfig.getPseudonymizationClientFactoryClass() + "' not found"));

		if ("dev.dsf.pseudonymization.client.stub.PseudonymizationClientStubFactory"
				.equals(factory.getClass().getName()))
			logger.warn("Using {} as pseudonymization client factory", factory.getClass().getName());
		else
			logger.info("Using {} as pseudonymization client factory", factory.getClass().getName());

		return factory;
	}
}
