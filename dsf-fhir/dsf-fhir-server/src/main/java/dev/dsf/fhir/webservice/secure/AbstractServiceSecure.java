package dev.dsf.fhir.webservice.secure;

import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.common.auth.conf.PractitionerIdentity;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.webservice.base.AbstractDelegatingBasicService;
import dev.dsf.fhir.webservice.base.BasicService;
import jakarta.ws.rs.core.Response;

public abstract class AbstractServiceSecure<S extends BasicService> extends AbstractDelegatingBasicService<S>
		implements InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractServiceSecure.class);
	protected static final Logger audit = LoggerFactory.getLogger("dsf-audit-logger");

	protected final String serverBase;
	protected final ResponseGenerator responseGenerator;
	protected final ReferenceResolver referenceResolver;

	public AbstractServiceSecure(S delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver)
	{
		super(delegate);

		this.serverBase = serverBase;
		this.referenceResolver = referenceResolver;
		this.responseGenerator = responseGenerator;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(serverBase, "serverBase");
		Objects.requireNonNull(responseGenerator, "responseGenerator");
		Objects.requireNonNull(referenceResolver, "referenceResolver");
	}

	protected final Response forbidden(String operation)
	{
		return responseGenerator.forbiddenNotAllowed(operation, currentIdentityProvider.getCurrentIdentity());
	}

	protected void logCurrentIdentity()
	{
		Identity identity = getCurrentIdentity();
		if (identity instanceof OrganizationIdentity)
		{
			logger.debug("Current organization identity '{}', dsf-roles '{}'", identity.getName(),
					identity.getDsfRoles());
		}
		else if (identity instanceof PractitionerIdentity practitioner)
		{
			logger.debug("Current practitioner identity '{}', dsf-roles '{}', practitioner-roles '{}'",
					identity.getName(), identity.getDsfRoles(), practitioner.getPractionerRoles().stream()
							.map(c -> c.getSystem() + "|" + c.getCode()).collect(Collectors.joining(", ", "[", "]")));
		}
	}
}
