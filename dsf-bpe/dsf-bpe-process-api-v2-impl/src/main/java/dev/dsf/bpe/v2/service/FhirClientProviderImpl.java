package dev.dsf.bpe.v2.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import dev.dsf.bpe.v2.client.fhir.ClientConfig;
import dev.dsf.bpe.v2.client.fhir.ClientConfigs;
import dev.dsf.bpe.v2.client.fhir.FhirClientFactory;
import dev.dsf.bpe.v2.config.ProxyConfig;

public class FhirClientProviderImpl implements FhirClientProvider, InitializingBean
{
	private final FhirContext fhirContext;
	private final ProxyConfig proxyConfig;
	private final OidcClientProvider oidcClientProvider;
	private final String userAgent;

	private final Map<String, ClientConfig> clientConfigsByFhirServerId = new HashMap<>();
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
	 * @param clientConfigs
	 *            may be <code>null</code>
	 */
	public FhirClientProviderImpl(FhirContext fhirContext, ProxyConfig proxyConfig,
			OidcClientProvider oidcClientProvider, String userAgent, ClientConfigs clientConfigs)
	{
		this.fhirContext = fhirContext;
		this.proxyConfig = proxyConfig;
		this.oidcClientProvider = oidcClientProvider;
		this.userAgent = userAgent;

		if (clientConfigs != null)
			clientConfigsByFhirServerId.putAll(clientConfigs.getConfigs().stream()
					.collect(Collectors.toMap(ClientConfig::getFhirServerId, Function.identity())));
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(fhirContext, "fhirContext");
		Objects.requireNonNull(proxyConfig, "proxyConfig");
		Objects.requireNonNull(oidcClientProvider, "oidcClientProvider");
		Objects.requireNonNull(userAgent, "userAgent");
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
		return getClientConfig(fhirServerId).flatMap(this::getClient);
	}

	@Override
	public Optional<ClientConfig> getClientConfig(String fhirServerId)
	{
		if (fhirServerId == null || fhirServerId.isBlank())
			return Optional.empty();

		return Optional.ofNullable(clientConfigsByFhirServerId.get(fhirServerId));
	}

	protected FhirClientFactory createClientFactory(ClientConfig config)
	{
		return new FhirClientFactory(oidcClientProvider, config, fhirContext, userAgent);
	}
}
