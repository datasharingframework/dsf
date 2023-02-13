package dev.dsf.openehr.client.impl;

import java.util.function.BiFunction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.dsf.openehr.client.OpenEhrClient;
import dev.dsf.openehr.client.OpenEhrClientFactory;
import dev.dsf.openehr.json.OpenEhrObjectMapperFactory;

public class OpenEhrClientJerseyFactory implements OpenEhrClientFactory
{
	private static final Logger logger = LoggerFactory.getLogger(OpenEhrClientJerseyFactory.class);

	@Override
	public OpenEhrClient createClient(BiFunction<String, String, String> propertyResolver)
	{
		ObjectMapper objectMapper = OpenEhrObjectMapperFactory.createObjectMapper();

		String baseUrl = propertyResolver.apply("dev.dsf.bpe.openehr.jersey.base.url", null);
		String basicAuthUsername = propertyResolver.apply("dev.dsf.bpe.openehr.jersey.basicauth.username", null);
		String basicAuthPassword = propertyResolver.apply("dev.dsf.bpe.openehr.jersey.basicauth.password", null);

		String trustCertificatesFile = propertyResolver.apply("dev.dsf.bpe.openehr.jersey.trust.certificates", null);

		int connectTimeout = Integer
				.parseInt(propertyResolver.apply("dev.dsf.bpe.openehr.jersey.timeout.connect", "2000"));
		int readTimeout = Integer.parseInt(propertyResolver.apply("dev.dsf.bpe.openehr.jersey.timeout.read", "10000"));

		try
		{
			return new OpenEhrClientJersey(baseUrl, basicAuthUsername, basicAuthPassword, trustCertificatesFile,
					connectTimeout, readTimeout, objectMapper);
		}
		catch (Exception exception)
		{
			logger.warn("Could not create OpenEhrClientJersey, reason: {}", exception.getMessage());
			throw new RuntimeException(exception);
		}
	}
}
