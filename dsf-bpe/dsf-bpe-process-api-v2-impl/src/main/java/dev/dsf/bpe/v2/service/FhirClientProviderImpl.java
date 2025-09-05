package dev.dsf.bpe.v2.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import dev.dsf.bpe.v2.client.fhir.ClientConfig;
import dev.dsf.bpe.v2.client.fhir.FhirClientFactory;
import dev.dsf.bpe.v2.config.ProxyConfig;

public class FhirClientProviderImpl implements FhirClientProvider, InitializingBean
{
	private final FhirContext fhirContext;
	private final ProxyConfig proxyConfig;
	private final OidcClientProvider oidcClientProvider;
	private final String userAgent;
	private final FhirClientConfigProvider configProvider;

	private final Map<String, FhirClientFactory> clientFactoriesByFhirServerId = new HashMap<>();

	/**
	 * @param fhirContext
	 *            not <code>null</code>
	 * @param proxyConfig
	 *            not <code>null</code>
	 * @param oidcClientProvider
	 *            not <code>null</code>
	 * @param userAgent
	 *            not <code>null</code>
	 * @param configProvider
	 *            not <code>null</code>
	 */
	public FhirClientProviderImpl(FhirContext fhirContext, ProxyConfig proxyConfig,
			OidcClientProvider oidcClientProvider, String userAgent, FhirClientConfigProvider configProvider)
	{
		this.fhirContext = fhirContext;
		this.proxyConfig = proxyConfig;
		this.oidcClientProvider = oidcClientProvider;
		this.userAgent = userAgent;
		this.configProvider = configProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(proxyConfig, "proxyConfig");
		Objects.requireNonNull(oidcClientProvider, "oidcClientProvider");
		Objects.requireNonNull(userAgent, "userAgent");
		Objects.requireNonNull(configProvider, "configProvider");
	}

	protected Optional<IGenericClient> getClient(ClientConfig clientConfig)
	{
		if (clientConfig == null)
			return Optional.empty();

		synchronized (clientFactoriesByFhirServerId)
		{
			FhirClientFactory factory = clientFactoriesByFhirServerId.get(clientConfig.getFhirServerId());

			if (factory == null)
			{
				factory = createClientFactory(clientConfig);
				clientFactoriesByFhirServerId.put(clientConfig.getFhirServerId(), factory);
			}

			return Optional.of(factory.newGenericClient(clientConfig.getBaseUrl()));
		}
	}

	@Override
	public Optional<IGenericClient> getClient(String fhirServerId)
	{
		return configProvider.getClientConfig(fhirServerId).flatMap(this::getClient);
	}

	protected FhirClientFactory createClientFactory(ClientConfig config)
	{
		return new FhirClientFactory(oidcClientProvider, config, fhirContext, userAgent);
	}
}
