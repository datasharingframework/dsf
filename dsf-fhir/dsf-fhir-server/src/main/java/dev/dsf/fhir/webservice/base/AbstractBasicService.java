package dev.dsf.fhir.webservice.base;

import dev.dsf.common.auth.Identity;
import dev.dsf.fhir.authentication.CurrentIdentityProvider;

public class AbstractBasicService implements BasicService
{
	protected CurrentIdentityProvider currentIdentityProvider;

	@Override
	public void setCurrentIdentityProvider(CurrentIdentityProvider currentIdentityProvider)
	{
		this.currentIdentityProvider = currentIdentityProvider;
	}

	protected final Identity getCurrentIdentity()
	{
		return currentIdentityProvider.getCurrentIdentity();
	}
}
