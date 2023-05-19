package dev.dsf.fhir.authentication;

import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;

import org.hl7.fhir.r4.model.Organization;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.dao.OrganizationDao;
import dev.dsf.fhir.help.ExceptionHandler;

public class OrganizationProviderImpl extends AbstractProvider implements OrganizationProvider, InitializingBean
{
	private final OrganizationDao dao;
	private final ExceptionHandler exceptionHandler;
	private final String localOrganizationIdentifierValue;

	public OrganizationProviderImpl(OrganizationDao dao, ExceptionHandler exceptionHandler,
			String localOrganizationIdentifierValue)
	{
		this.dao = dao;
		this.exceptionHandler = exceptionHandler;
		this.localOrganizationIdentifierValue = localOrganizationIdentifierValue;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(dao, "dao");
		Objects.requireNonNull(exceptionHandler, "exceptionHandler");
		Objects.requireNonNull(localOrganizationIdentifierValue, "localOrganizationIdentifierValue");
	}

	private Optional<Organization> getOrganization(String thumbprint)
	{
		return exceptionHandler.catchAndLogSqlExceptionAndIfReturn(
				() -> dao.readActiveNotDeletedByThumbprint(thumbprint), Optional::empty);
	}

	@Override
	public Optional<Organization> getOrganization(X509Certificate certificate)
	{
		if (certificate == null)
			return Optional.empty();

		String thumbprint = getThumbprint(certificate);
		return getOrganization(thumbprint);
	}

	@Override
	public Optional<Organization> getLocalOrganization()
	{
		return exceptionHandler.catchAndLogSqlExceptionAndIfReturn(
				() -> dao.readActiveNotDeletedByIdentifier(localOrganizationIdentifierValue), Optional::empty);
	}

	@Override
	public String getLocalOrganizationIdentifierValue()
	{
		return localOrganizationIdentifierValue;
	}

	@Override
	public Optional<Identity> getLocalOrganizationAsIdentity()
	{
		return getLocalOrganization().map(o -> new OrganizationIdentityImpl(true, o, Collections.emptySet(), null));
	}
}
