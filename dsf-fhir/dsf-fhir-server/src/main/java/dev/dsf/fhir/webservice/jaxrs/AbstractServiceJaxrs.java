package dev.dsf.fhir.webservice.jaxrs;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

import org.springframework.beans.factory.InitializingBean;

import dev.dsf.fhir.authentication.UserProvider;
import dev.dsf.fhir.webservice.base.AbstractDelegatingBasicService;
import dev.dsf.fhir.webservice.base.BasicService;

public abstract class AbstractServiceJaxrs<S extends BasicService> extends AbstractDelegatingBasicService<S>
		implements BasicService, InitializingBean
{
	@Context
	private volatile HttpServletRequest httpRequest;

	public AbstractServiceJaxrs(S delegate)
	{
		super(delegate);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		setUserProvider(new UserProvider(() -> httpRequest));
	}
}
