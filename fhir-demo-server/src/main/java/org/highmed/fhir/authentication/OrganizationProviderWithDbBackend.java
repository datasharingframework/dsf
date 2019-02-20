package org.highmed.fhir.authentication;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.codec.binary.Hex;
import org.highmed.fhir.dao.OrganizationDao;
import org.hl7.fhir.r4.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

public class OrganizationProviderWithDbBackend implements OrganizationProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(OrganizationProviderWithDbBackend.class);

	private final OrganizationDao dao;
	private final List<String> localUserThumbprints = new ArrayList<String>();

	public OrganizationProviderWithDbBackend(OrganizationDao dao, List<String> localUserThumbprints)
	{
		this.dao = dao;
		if (localUserThumbprints != null)
			this.localUserThumbprints.addAll(localUserThumbprints);
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(dao, "dao");
		if (localUserThumbprints.isEmpty())
			logger.warn("No local users configured");
		else
			logger.info("{} local user{} configured with tumbprint{}: {}", localUserThumbprints.size(),
					localUserThumbprints.size() != 1 ? "s" : "", localUserThumbprints.size() != 1 ? "s" : "",
					localUserThumbprints.stream().collect(Collectors.joining(", ", "[", "]")));
	}

	@Override
	public Optional<Organization> getOrganization(X509Certificate certificate)
	{
		if (certificate == null)
			return Optional.empty();

		String loginThumbprintHex = Hex.encodeHexString(getThumbprint(certificate));
		logger.debug("Generated SHA-512 certificate thumbprint: {}", loginThumbprintHex);

		if (localUserThumbprints.contains(loginThumbprintHex))
			return Optional.of(new Organization().setName("Local User"));
		else
			return dao.readByIdentifier("", loginThumbprintHex);
	}

	private byte[] getThumbprint(X509Certificate certificate)
	{
		try
		{
			return MessageDigest.getInstance("SHA-512").digest(certificate.getEncoded());
		}
		catch (CertificateEncodingException | NoSuchAlgorithmException e)
		{
			throw new RuntimeException(e);
		}
	}
}
