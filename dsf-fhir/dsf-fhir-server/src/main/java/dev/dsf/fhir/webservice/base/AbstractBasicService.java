package dev.dsf.fhir.webservice.base;

import dev.dsf.fhir.authentication.User;
import dev.dsf.fhir.authentication.UserProvider;

public class AbstractBasicService implements BasicService
{
	protected UserProvider userProvider;

	@Override
	public final void setUserProvider(UserProvider userProvider)
	{
		this.userProvider = userProvider;
	}

	protected final User getCurrentUser()
	{
		return userProvider.getCurrentUser();
	}
}
