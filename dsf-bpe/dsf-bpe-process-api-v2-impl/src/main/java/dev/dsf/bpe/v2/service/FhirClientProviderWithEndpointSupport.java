package dev.dsf.bpe.v2.service;

import java.security.KeyStore;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import dev.dsf.bpe.api.config.FhirClientConfig;
import dev.dsf.bpe.v2.client.fhir.ClientConfig;

public class FhirClientProviderWithEndpointSupport implements FhirClientProvider, InitializingBean
{
	private final EndpointProvider endpointProvider;
	private final FhirClientProviderImpl delegate;

	public FhirClientProviderWithEndpointSupport(EndpointProvider endpointProvider, FhirClientProviderImpl delegate)
	{
		this.endpointProvider = endpointProvider;
		this.delegate = delegate;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(endpointProvider, "endpointProvider");
		Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public Optional<IGenericClient> getClient(String fhirServerId)
	{
		if (fhirServerId == null || fhirServerId.isBlank())
			return Optional.empty();
		else if (fhirServerId.startsWith("#"))
			return getClientConfig(fhirServerId).flatMap(delegate::getClient);
		else
			return delegate.getClient(fhirServerId);
	}

	@Override
	public Optional<ClientConfig> getClientConfig(String fhirServerId)
	{
		if (fhirServerId == null || fhirServerId.isBlank())
			return Optional.empty();
		else if ("#local".equals(fhirServerId))
			return delegate.getClientConfig(FhirClientConfig.DSF_CLIENT_FHIR_SERVER_ID)
					.map(withBaseUrlAndFhirServerId(endpointProvider.getLocalEndpointAddress(), fhirServerId));
		else if (fhirServerId.startsWith("#"))
			return endpointProvider.getEndpointAddress(fhirServerId.substring(1, fhirServerId.length()))
					.flatMap(address -> delegate.getClientConfig(FhirClientConfig.DSF_CLIENT_FHIR_SERVER_ID)
							.map(withBaseUrlAndFhirServerId(address, fhirServerId)));
		else
			return delegate.getClientConfig(fhirServerId);
	}

	private Function<ClientConfig, ClientConfig> withBaseUrlAndFhirServerId(String baseUrl, String fhirServerId)
	{
		return delegate -> new ClientConfig()
		{
			@Override
			public String getFhirServerId()
			{
				return fhirServerId;
			}

			@Override
			public String getBaseUrl()
			{
				return baseUrl;
			}

			@Override
			public boolean getTestConnectionOnStartup()
			{
				return delegate.getTestConnectionOnStartup();
			}

			@Override
			public boolean getEnableDebugLogging()
			{
				return delegate.getEnableDebugLogging();
			}

			@Override
			public Duration getConnectTimeout()
			{
				return delegate.getConnectTimeout();
			}

			@Override
			public Duration getReadTimeout()
			{
				return delegate.getReadTimeout();
			}

			@Override
			public KeyStore getTrustStore()
			{
				return delegate.getTrustStore();
			}

			@Override
			public CertificateAuthentication getCertificateAuthentication()
			{
				return delegate.getCertificateAuthentication();
			}

			@Override
			public BasicAuthentication getBasicAuthentication()
			{
				return delegate.getBasicAuthentication();
			}

			@Override
			public BearerAuthentication getBearerAuthentication()
			{
				return delegate.getBearerAuthentication();
			}

			@Override
			public OidcAuthentication getOidcAuthentication()
			{
				return delegate.getOidcAuthentication();
			}

			@Override
			public Proxy getProxy()
			{
				return delegate.getProxy();
			}
		};
	}
}
