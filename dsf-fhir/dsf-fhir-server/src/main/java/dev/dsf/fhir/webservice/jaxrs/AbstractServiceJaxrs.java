package dev.dsf.fhir.webservice.jaxrs;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.auth.Identity;
import dev.dsf.fhir.webservice.base.AbstractDelegatingBasicService;
import dev.dsf.fhir.webservice.base.BasicService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;

public abstract class AbstractServiceJaxrs<S extends BasicService> extends AbstractDelegatingBasicService<S>
		implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractServiceJaxrs.class);

	@Context
	private volatile HttpServletRequest httpRequest;

	public AbstractServiceJaxrs(S delegate)
	{
		super(delegate);
	}

	private Identity doGetCurrentIdentity()
	{
		Principal principal = httpRequest.getUserPrincipal();
		if (principal != null)
		{
			if (principal instanceof Identity)
				return (Identity) principal;
			else
			{
				logger.warn("Unknown current user principal of type {}", principal.getClass().getName());
				return null;
			}
		}
		else
			return null;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		setCurrentIdentityProvider(this::doGetCurrentIdentity);
	}
}
