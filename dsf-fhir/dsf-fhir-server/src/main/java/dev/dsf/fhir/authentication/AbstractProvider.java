package dev.dsf.fhir.authentication;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.codec.binary.Hex;
import org.hl7.fhir.r4.model.Identifier;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.fhir.help.ExceptionHandler;

public abstract class AbstractProvider implements InitializingBean
{
	protected final ExceptionHandler exceptionHandler;

	public AbstractProvider(ExceptionHandler exceptionHandler)
	{
		this.exceptionHandler = exceptionHandler;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(exceptionHandler, "exceptionHandler");
	}

	protected final Optional<String> getIdentifierValue(List<Identifier> identifiers, String system)
	{
		return identifiers.stream().filter(Identifier::hasSystem).filter(Identifier::hasValue)
				.filter(i -> system.equals(i.getSystem())).map(Identifier::getValue).findFirst();
	}

	protected final String getThumbprint(X509Certificate certificate)
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
}
