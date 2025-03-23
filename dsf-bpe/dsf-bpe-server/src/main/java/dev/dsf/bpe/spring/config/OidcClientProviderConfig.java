package dev.dsf.bpe.spring.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.dsf.bpe.api.service.BpeOidcClientProvider;
import dev.dsf.bpe.client.oidc.OidcClientProviderImpl;

@Configuration
public class OidcClientProviderConfig
{
	@Autowired
	private PropertiesConfig propertiesConfig;

	@Autowired
	private BuildInfoReaderConfig buildInfoReaderConfig;

	@Bean
	public BpeOidcClientProvider bpeOidcClientProvider()
	{
		return new OidcClientProviderImpl(propertiesConfig.proxyConfig(),
				propertiesConfig.getFhirClientConnectionsConfigDefaultOidcDiscoveryPath(),
				propertiesConfig.getFhirClientConnectionsConfigDefaultConnectTimeout(),
				propertiesConfig.getFhirClientConnectionsConfigDefaultReadTimeout(),
				propertiesConfig.getFhirClientConnectionsConfigDefaultTrustStore(),
				propertiesConfig.getFhirClientConnectionsConfigDefaultEnableDebugLogging(),
				buildInfoReaderConfig.buildInfoReader().getUserAgentValue(),
				propertiesConfig.getFhirClientConnectionsConfigOidcClientCacheEnabled(),
				propertiesConfig.getFhirClientConnectionsConfigOidcClientCacheConfigurationResourceTimeout(),
				propertiesConfig.getFhirClientConnectionsConfigOidcClientCacheJwksResourceTimeout(),
				propertiesConfig.getFhirClientConnectionsConfigOidcClientCacheAccessTokenBeforeExpirationTimeout(),
				propertiesConfig.getFhirClientConnectionsConfigOidcClientNotBeforeIssuedAtExpiresAtLeeway());
	}
}
