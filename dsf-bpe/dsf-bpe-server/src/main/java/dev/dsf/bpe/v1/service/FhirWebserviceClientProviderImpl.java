package dev.dsf.bpe.v1.service;

import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.client.FhirClientProvider;
import dev.dsf.fhir.client.FhirWebserviceClient;

public class FhirWebserviceClientProviderImpl implements FhirWebserviceClientProvider, InitializingBean
{
	private final FhirClientProvider delegate;

	public FhirWebserviceClientProviderImpl(FhirClientProvider delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public FhirWebserviceClient getLocalWebserviceClient()
	{
		return delegate.getLocalWebserviceClient();
	}

	@Override
	public FhirWebserviceClient getWebserviceClient(String webserviceUrl)
	{
		return delegate.getWebserviceClient(webserviceUrl);
	}
}
