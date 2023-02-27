package dev.dsf.fhir.webservice.base;

import java.util.Objects;

import org.springframework.beans.factory.InitializingBean;

import dev.dsf.fhir.authentication.User;
import dev.dsf.fhir.authentication.UserProvider;

public class AbstractDelegatingBasicService<S extends BasicService> implements BasicService, InitializingBean
{
	protected final S delegate;

	protected UserProvider userProvider;

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
	public final void setUserProvider(UserProvider userProvider)
	{
		delegate.setUserProvider(userProvider);

		this.userProvider = userProvider;
	}

	protected final User getCurrentUser()
	{
		return userProvider.getCurrentUser();
	}
}
