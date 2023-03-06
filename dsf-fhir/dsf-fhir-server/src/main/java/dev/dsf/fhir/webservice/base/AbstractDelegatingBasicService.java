package dev.dsf.fhir.webservice.base;

import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;

import dev.dsf.fhir.authentication.CurrentIdentityProvider;

public class AbstractDelegatingBasicService<S extends BasicService> extends AbstractBasicService
		implements BasicService, InitializingBean
{
	protected final S delegate;

	public AbstractDelegatingBasicService(S delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(delegate, "delegate");
	}

	@Override
	public void setCurrentIdentityProvider(CurrentIdentityProvider currentIdentityProvider)
	{
		super.setCurrentIdentityProvider(currentIdentityProvider);
		delegate.setCurrentIdentityProvider(currentIdentityProvider);
	}
}
