package dev.dsf.bpe.api.config;

import java.security.KeyStore;
import java.util.List;
import java.util.Optional;

public interface FhirClientConfigs
{
	/**
	 * @return never <code>null</code>, arbitrary order
	 */
	List<FhirClientConfig> getConfigs();

	/**
	 * @param fhirServerId
	 *            may be <code>null</code>
	 * @return never <code>null</code>, {@link Optional#isEmpty()} if not found or given <b>fhirServerId</b> null
	 */
	Optional<FhirClientConfig> getConfig(String fhirServerId);

	/**
	 * @param config
	 *            not <code>null</code>
	 * @return a new {@link FhirClientConfigs} with the added {@link FhirClientConfig}
	 */
	FhirClientConfigs addConfig(FhirClientConfig config);

	/**
	 * @return default trust store used with {@link FhirClientConfig}s
	 */
	KeyStore defaultTrustStore();
}
