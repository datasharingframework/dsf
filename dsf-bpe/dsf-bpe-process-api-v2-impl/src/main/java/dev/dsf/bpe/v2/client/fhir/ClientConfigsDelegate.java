package dev.dsf.bpe.v2.client.fhir;

import java.util.List;

import dev.dsf.bpe.api.config.BpeProxyConfig;
import dev.dsf.bpe.api.config.FhirClientConfigs;

public class ClientConfigsDelegate implements ClientConfigs
{
	private final FhirClientConfigs delegate;
	private final BpeProxyConfig proxyConfig;

	public ClientConfigsDelegate(FhirClientConfigs delegate, BpeProxyConfig proxyConfig)
	{
		this.delegate = delegate;
		this.proxyConfig = proxyConfig;
	}

	@Override
	public List<ClientConfig> getConfigs()
	{
		return delegate.getConfigs().stream().map(d -> new ClientConfigDelegate(d, proxyConfig))
				.map(c -> (ClientConfig) c).toList();
	}
}
