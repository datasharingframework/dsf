package dev.dsf.fhir.authentication;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.DsfOpenIdCredentials;

public class PractitionerProviderImpl extends AbstractProvider implements PractitionerProvider
{
	private static final Logger logger = LoggerFactory.getLogger(PractitionerProviderImpl.class);

	private final List<String> configuredUserThumbprints = new ArrayList<>();

	public PractitionerProviderImpl(List<String> configuredUserThumbprints)
	{
		if (configuredUserThumbprints != null)
			this.configuredUserThumbprints.addAll(configuredUserThumbprints);
	}

	@Override
	public Optional<Practitioner> getPractitioner(DsfOpenIdCredentials credentials)
	{
		if (credentials == null)
			return Optional.empty();

		return Optional.of(toPractitioner(credentials));
	}

	@Override
	public Optional<Practitioner> getPractitioner(X509Certificate certificate)
	{
		if (certificate == null)
			return Optional.empty();

		String thumbprint = getThumbprint(certificate);
		if (!configuredUserThumbprints.contains(thumbprint))
			return Optional.empty();

		return toJcaX509CertificateHolder(certificate).map(this::toPractitioner);
	}

	private Optional<JcaX509CertificateHolder> toJcaX509CertificateHolder(X509Certificate certificate)
	{
		try
		{
			return Optional.of(new JcaX509CertificateHolder(certificate));
		}
		catch (CertificateEncodingException e)
		{
			logger.warn("Unable to get X500Name from certificate: {} - {}", e.getClass().getName(), e.getMessage());
			return Optional.empty();
		}
	}

	private Practitioner toPractitioner(JcaX509CertificateHolder certificate)
	{
		X500Name subject = certificate.getSubject();
		List<String> givennames = getValues(subject, BCStyle.GIVENNAME);
		List<String> surnames = getValues(subject, BCStyle.SURNAME);
		List<String> commonName = getValues(subject, BCStyle.CN);
		List<String> email1 = getValues(subject, BCStyle.E);
		List<String> email2 = getValues(subject, BCStyle.EmailAddress);

		Extension subjectAlternativeNames = certificate.getExtension(Extension.subjectAlternativeName);
		List<String> rfc822Names = subjectAlternativeNames == null ? Collections.emptyList()
				: Stream.of(GeneralNames.getInstance(subjectAlternativeNames.getParsedValue()).getNames())
						.filter(n -> n.getTagNo() == GeneralName.rfc822Name).map(GeneralName::getName)
						.map(IETFUtils::valueToString).toList();

		Stream<String> emails = Stream.concat(Stream.concat(email1.stream(), email2.stream()), rfc822Names.stream());
		return toPractitioner(!surnames.isEmpty() ? surnames.stream() : commonName.stream(), givennames.stream(),
				emails);
	}

	private Practitioner toPractitioner(DsfOpenIdCredentials credentials)
	{
		String iss = credentials.getStringClaimOrDefault("iss", "");
		String sub = credentials.getStringClaimOrDefault("sub", "");

		Stream<String> surname = Stream.of((String) credentials.getStringClaimOrDefault("family_name", ""));
		Stream<String> givenNames = Stream.of((String) credentials.getStringClaimOrDefault("given_name", ""));
		Stream<String> emails = Stream.of((String) credentials.getStringClaimOrDefault("email", ""), toEmail(iss, sub));

		return toPractitioner(surname, givenNames, emails);
	}

	private String toEmail(String iss, String sub)
	{
		try
		{
			return sub + "@" + new URL(iss).getHost();
		}
		catch (MalformedURLException e)
		{
			return null;
		}
	}

	private Practitioner toPractitioner(Stream<String> surname, Stream<String> givenNames, Stream<String> emails)
	{
		Practitioner practitioner = new Practitioner();

		emails.filter(e -> e != null).filter(e -> e.contains("@"))
				.map(e -> new Identifier().setSystem(PRACTITIONER_IDENTIFIER_SYSTEM).setValue(e))
				.forEach(practitioner::addIdentifier);

		HumanName name = new HumanName();
		name.setFamily(surname.collect(Collectors.joining(" ")));
		givenNames.forEach(name::addGiven);
		practitioner.addName(name);

		return practitioner;
	}

	private List<String> getValues(X500Name name, ASN1ObjectIdentifier attribute)
	{
		return Stream.of(name.getRDNs(attribute)).flatMap(rdn -> Stream.of(rdn.getTypesAndValues()))
				.map(AttributeTypeAndValue::getValue).map(IETFUtils::valueToString).toList();
	}
}
