/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
