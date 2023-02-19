package dev.dsf.fhir.webservice.base;

import dev.dsf.fhir.authentication.CurrentIdentityProvider;

public interface BasicService
{
	void setCurrentIdentityProvider(CurrentIdentityProvider currentIdentityProvider);
}
