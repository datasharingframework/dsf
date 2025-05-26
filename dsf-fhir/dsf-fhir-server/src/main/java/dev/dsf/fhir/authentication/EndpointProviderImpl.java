package dev.dsf.fhir.authentication;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.fhir.dao.EndpointDao;
import dev.dsf.fhir.help.ExceptionHandler;

public class EndpointProviderImpl extends AbstractProvider implements EndpointProvider, InitializingBean
{
	private final EndpointDao dao;
	private final String serverBaseUrl;

	public EndpointProviderImpl(ExceptionHandler exceptionHandler, EndpointDao dao, String serverBaseUrl)
	{
		super(exceptionHandler);

		this.dao = dao;
		this.serverBaseUrl = serverBaseUrl;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(dao, "dao");
		Objects.requireNonNull(serverBaseUrl, "serverBaseUrl");
	}

	@Override
	public Optional<Endpoint> getLocalEndpoint()
	{
		return exceptionHandler.catchAndLogSqlExceptionAndIfReturn(
				() -> dao.readActiveNotDeletedByAddress(serverBaseUrl), Optional::empty);
	}

	@Override
	public Optional<String> getLocalEndpointIdentifierValue()
	{
		return getLocalEndpoint().filter(Endpoint::hasIdentifier).map(Endpoint::getIdentifier)
				.flatMap(ids -> getIdentifierValue(ids, ENDPOINT_IDENTIFIER_SYSTEM));
	}

	@Override
	public Optional<Endpoint> getEndpoint(Organization organization, X509Certificate x509Certificate)
	{
		String thumbprint = getThumbprint(x509Certificate);

		Optional<Endpoint> endpoint = exceptionHandler.catchAndLogSqlExceptionAndIfReturn(
				() -> dao.readActiveNotDeletedByThumbprint(thumbprint), Optional::empty);

		if (endpoint.isEmpty())
		{
			List<Reference> endpoints = organization.getEndpoint();
			if (endpoints.size() == 1 && endpoints.get(0).hasReference())
			{
				String id = endpoints.get(0).getReferenceElement().getIdPart();

				endpoint = exceptionHandler.catchAndLogSqlAndResourceDeletedExceptionAndIfReturn(
						() -> dao.read(UUID.fromString(id)), Optional::empty, Optional::empty);
			}
		}

		return endpoint;
	}
}
