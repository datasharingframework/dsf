package dev.dsf.fhir.webservice.base;

import dev.dsf.fhir.authentication.UserProvider;

public interface BasicService
{
	void setUserProvider(UserProvider provider);
}
