package dev.dsf.fhir.authentication;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.codec.binary.Hex;
import org.hl7.fhir.r4.model.Organization;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.OrganizationIdentityImpl;
import dev.dsf.fhir.dao.OrganizationDao;
import dev.dsf.fhir.help.ExceptionHandler;

public class OrganizationProviderImpl implements OrganizationProvider, InitializingBean
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

	protected String getThumbprint(X509Certificate certificate)
	{
		try
		{
			byte[] digest = MessageDigest.getInstance("SHA-512").digest(certificate.getEncoded());
			return Hex.encodeHexString(digest);
		}
		catch (CertificateEncodingException | NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}
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
		return getLocalOrganization().map(o -> new OrganizationIdentityImpl(true, o, List.of(), null));
	}
}
