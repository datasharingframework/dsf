package dev.dsf.fhir.authentication;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.hl7.fhir.r4.model.Organization;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.OrganizationIdentityImpl;
import dev.dsf.fhir.dao.OrganizationDao;
import dev.dsf.fhir.help.ExceptionHandler;

public class OrganizationProviderImpl extends AbstractProvider implements OrganizationProvider, InitializingBean
{
	private final OrganizationDao dao;
	private final String localOrganizationIdentifierValue;

	public OrganizationProviderImpl(ExceptionHandler exceptionHandler, OrganizationDao dao,
			String localOrganizationIdentifierValue)
	{
		super(exceptionHandler);

		this.dao = dao;
		this.localOrganizationIdentifierValue = localOrganizationIdentifierValue;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(dao, "dao");
		Objects.requireNonNull(localOrganizationIdentifierValue, "localOrganizationIdentifierValue");
	}

	@Override
	public Optional<Organization> getOrganization(X509Certificate certificate)
	{
		if (certificate == null)
			return Optional.empty();

		String thumbprint = getThumbprint(certificate);

		return exceptionHandler.catchAndLogSqlExceptionAndIfReturn(
				() -> dao.readActiveNotDeletedByThumbprint(thumbprint), Optional::empty);
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
		return getLocalOrganization().map(o -> new OrganizationIdentityImpl(true, o, null, List.of(), null));
	}
}
