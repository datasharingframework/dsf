package dev.dsf.bpe.config;

import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import dev.dsf.bpe.api.config.FhirClientConfig;
import dev.dsf.bpe.api.config.FhirClientConfigs;

public record FhirClientConfigsImpl(Map<String, FhirClientConfig> configs, KeyStore defaultTrustStore)
		implements FhirClientConfigs
{
	public static FhirClientConfigs empty(KeyStore defaultTrustStore)
	{
		return new FhirClientConfigsImpl(Map.of(), defaultTrustStore);
	}

	@Override
	public List<FhirClientConfig> getConfigs()
	{
		return List.copyOf(configs.values());
	}

	@Override
	public Optional<FhirClientConfig> getConfig(String fhirServerId)
	{
		return fhirServerId != null ? Optional.ofNullable(configs.get(fhirServerId)) : Optional.empty();
	}

	@Override
	public FhirClientConfigs addConfig(FhirClientConfig config)
	{
		Objects.requireNonNull(config, "config");

		Map<String, FhirClientConfig> map = new HashMap<>(configs);
		map.put(config.fhirServerId(), config);
		return new FhirClientConfigsImpl(map, defaultTrustStore);
	}
}
