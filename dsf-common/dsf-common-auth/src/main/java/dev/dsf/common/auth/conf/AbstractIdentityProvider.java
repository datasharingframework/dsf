package dev.dsf.common.auth.conf;

import java.net.URI;
import java.net.URISyntaxException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.codec.binary.Hex;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.AttributeTypeAndValue;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.auth.DsfOpenIdCredentials;
import dev.dsf.common.auth.conf.RoleConfig.Mapping;

public abstract class AbstractIdentityProvider<R extends DsfRole> implements IdentityProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractIdentityProvider.class);

	private static final String PRACTITIONER_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/practitioner-identifier";

	private final RoleConfig<R> roleConfig;
	private final Set<String> thumbprints;

	public AbstractIdentityProvider(RoleConfig<R> roleConfig)
	{
		this.roleConfig = roleConfig;

		thumbprints = roleConfig.getEntries().stream().map(Mapping::getThumbprints).flatMap(List::stream).distinct()
				.collect(Collectors.toUnmodifiableSet());
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(roleConfig, "roleConfig");
	}

	@Override
	public final Identity getIdentity(DsfOpenIdCredentials credentials)
	{
		if (credentials == null)
			return null;

		Optional<Practitioner> practitioner = toPractitioner(credentials);
		Optional<Organization> localOrganization = getLocalOrganization();

		if (practitioner.isPresent() && localOrganization.isPresent())
		{
			Map<String, Object> parsedIdToken = credentials.getIdToken();
			Map<String, Object> parsedAccessToken = credentials.getAccessToken();

			List<String> rolesFromTokens = getRolesFromTokens(parsedIdToken, parsedAccessToken);
			List<String> groupsFromTokens = getGroupsFromTokens(parsedIdToken, parsedAccessToken);

			Set<R> dsfRoles = getDsfRolesFor(practitioner.get(), null, rolesFromTokens, groupsFromTokens);
			Set<Coding> practitionerRoles = getPractitionerRolesFor(practitioner.get(), null, rolesFromTokens,
					groupsFromTokens);

			if (dsfRoles.isEmpty())
			{
				logger.warn("User from OpenID Connect token '{}' not configured as local user",
						credentials.getUserId());
				return null;
			}

			Optional<Endpoint> localEndpoint = getLocalEndpoint();

			return new PractitionerIdentityImpl(localOrganization.get(), localEndpoint.orElse(null), dsfRoles, null,
					practitioner.get(), practitionerRoles, credentials);
		}
		else
		{
			logger.warn(
					"User from OpenID Connect token '{}' not configured as local user or local organization unknown",
					credentials.getUserId());
			return null;
		}
	}

	protected abstract Optional<Organization> getLocalOrganization();

	protected abstract Optional<Endpoint> getLocalEndpoint();

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

	protected final String getDn(X509Certificate certificate)
	{
		return certificate.getSubjectX500Principal().getName(X500Principal.RFC1779);
	}

	protected final List<String> getGroupsFromTokens(Map<String, Object> parsedIdToken,
			Map<String, Object> parsedAccessToken)
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("id_token: groups: {}", getPropertyArray(parsedIdToken, "groups"));
			logger.debug("access_token: groups: {}", getPropertyArray(parsedAccessToken, "groups"));
		}

		return Stream.concat(getPropertyArray(parsedIdToken, "groups").stream(),
				getPropertyArray(parsedAccessToken, "groups").stream()).toList();
	}

	protected final List<String> getRolesFromTokens(Map<String, Object> idToken, Map<String, Object> accessToken)
	{
		return Stream.concat(getRolesFromToken("id_token", idToken), getRolesFromToken("access_token", accessToken))
				.toList();
	}

	@SuppressWarnings("unchecked")
	private Stream<String> getRolesFromToken(String tokenName, Map<String, Object> token)
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("{}: realm_access.roles: {}", tokenName,
					getPropertyArray(getPropertyMap(token, "realm_access"), "roles"));
			logger.debug("{}: resource_access.*.roles: {}", tokenName,
					getPropertyMap(token, "resource_access").entrySet().stream()
							.flatMap(e -> getPropertyArray((Map<String, Object>) e.getValue(), "roles").stream()
									.map(r -> e.getKey() + "." + r))
							.toList());
		}

		return Stream.concat(getPropertyArray(getPropertyMap(token, "realm_access"), "roles").stream(),
				getPropertyMap(token, "resource_access").entrySet().stream()
						.flatMap(e -> getPropertyArray((Map<String, Object>) e.getValue(), "roles").stream()
								.map(r -> e.getKey() + "." + r)));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getPropertyMap(Map<String, Object> map, String property)
	{
		Object propertyValue = map.get(property);
		if (propertyValue != null && propertyValue instanceof Map m)
			return m;
		else
			return Map.of();
	}

	private List<String> getPropertyArray(Map<String, Object> map, String property)
	{
		Object propertyValue = map.get(property);
		if (propertyValue != null && propertyValue instanceof Object[] o)
			return Arrays.stream(o).filter(v -> v instanceof String).map(v -> (String) v).toList();
		else
			return List.of();
	}

	// thumbprint from certificate, token roles and groups from jwt
	protected final Set<R> getDsfRolesFor(Practitioner practitioner, String thumbprint, List<String> tokenRoles,
			List<String> tokenGroups)
	{
		List<String> emailAddresses = practitioner.getIdentifier().stream()
				.filter(i -> PRACTITIONER_IDENTIFIER_SYSTEM.equals(i.getSystem()) && i.hasValue())
				.map(Identifier::getValue).toList();

		Stream<R> r1 = emailAddresses.stream().map(roleConfig::getDsfRolesForEmail).flatMap(List::stream);
		Stream<R> r2 = thumbprint == null ? Stream.empty() : roleConfig.getDsfRolesForThumbprint(thumbprint).stream();
		Stream<R> r3 = tokenRoles == null ? Stream.empty()
				: tokenRoles.stream().map(roleConfig::getDsfRolesForTokenRole).flatMap(List::stream);
		Stream<R> r4 = tokenGroups == null ? Stream.empty()
				: tokenGroups.stream().map(roleConfig::getDsfRolesForTokenGroup).flatMap(List::stream);

		return Stream.of(r1, r2, r3, r4).flatMap(Function.identity()).distinct().collect(Collectors.toSet());
	}

	// thumbprint from certificate, token roles and groups from jwt
	protected final Set<Coding> getPractitionerRolesFor(Practitioner practitioner, String thumbprint,
			List<String> tokenRoles, List<String> tokenGroups)
	{
		List<String> emailAddresses = practitioner.getIdentifier().stream()
				.filter(i -> PRACTITIONER_IDENTIFIER_SYSTEM.equals(i.getSystem()) && i.hasValue())
				.map(Identifier::getValue).toList();

		Stream<Coding> r1 = emailAddresses.stream().map(roleConfig::getPractitionerRolesForEmail).flatMap(List::stream);
		Stream<Coding> r2 = thumbprint == null ? Stream.empty()
				: roleConfig.getPractitionerRolesForThumbprint(thumbprint).stream();
		Stream<Coding> r3 = tokenRoles == null ? Stream.empty()
				: tokenRoles.stream().map(roleConfig::getPractitionerRolesForTokenRole).flatMap(List::stream);
		Stream<Coding> r4 = tokenGroups == null ? Stream.empty()
				: tokenGroups.stream().map(roleConfig::getPractitionerRolesForTokenGroup).flatMap(List::stream);

		return Stream.of(r1, r2, r3, r4).flatMap(Function.identity()).distinct().collect(Collectors.toSet());
	}

	protected final Optional<Practitioner> toPractitioner(DsfOpenIdCredentials credentials)
	{
		if (credentials == null)
			return Optional.empty();

		String iss = credentials.getStringClaimOrDefault("iss", "");
		String sub = credentials.getStringClaimOrDefault("sub", "");

		Set<String> emails = Stream.of(credentials.getStringClaimOrDefault("email", ""), toEmail(iss, sub))
				.filter(m -> m != null).distinct().collect(Collectors.toSet());

		Stream<String> surname = Stream.of(credentials.getStringClaimOrDefault("family_name", ""));
		Stream<String> givenNames = Stream.of(credentials.getStringClaimOrDefault("given_name", ""));

		return toPractitioner(surname, givenNames, emails.stream());
	}

	private Optional<Practitioner> toPractitioner(Stream<String> surname, Stream<String> givenNames,
			Stream<String> emails)
	{
		Practitioner practitioner = new Practitioner();

		emails.filter(e -> e != null).filter(e -> e.contains("@"))
				.map(e -> new Identifier().setSystem(PRACTITIONER_IDENTIFIER_SYSTEM).setValue(e))
				.forEach(practitioner::addIdentifier);

		HumanName name = new HumanName();
		name.setFamily(surname.collect(Collectors.joining(" ")));
		givenNames.forEach(name::addGiven);
		practitioner.addName(name);

		return Optional.of(practitioner);
	}

	private String toEmail(String iss, String sub)
	{
		if (iss == null || sub == null || iss.isBlank() || sub.isBlank())
			return null;

		try
		{
			return sub + "@" + new URI(iss).getHost();
		}
		catch (URISyntaxException e)
		{
			return null;
		}
	}

	protected final Optional<Practitioner> toPractitioner(X509Certificate certificate)
	{
		if (certificate == null)
			return Optional.empty();

		String thumbprint = getThumbprint(certificate);
		if (!thumbprints.contains(thumbprint))
			return Optional.empty();

		return toJcaX509CertificateHolder(certificate).flatMap(this::toPractitioner);
	}

	private Optional<JcaX509CertificateHolder> toJcaX509CertificateHolder(X509Certificate certificate)
	{
		try
		{
			return Optional.of(new JcaX509CertificateHolder(certificate));
		}
		catch (CertificateEncodingException e)
		{
			logger.debug("Unable to decode certificate", e);
			logger.warn("Unable to decode certificate: {} - {}", e.getClass().getName(), e.getMessage());

			return Optional.empty();
		}
	}

	private Optional<Practitioner> toPractitioner(JcaX509CertificateHolder certificate)
	{
		X500Name subject = certificate.getSubject();
		List<String> givennames = getValues(subject, BCStyle.GIVENNAME);
		List<String> surnames = getValues(subject, BCStyle.SURNAME);
		List<String> commonName = getValues(subject, BCStyle.CN);
		List<String> email1 = getValues(subject, BCStyle.E);
		List<String> email2 = getValues(subject, BCStyle.EmailAddress);

		Extension subjectAlternativeNames = certificate.getExtension(Extension.subjectAlternativeName);
		List<String> rfc822Names = subjectAlternativeNames == null ? List.of()
				: Stream.of(GeneralNames.getInstance(subjectAlternativeNames.getParsedValue()).getNames())
						.filter(n -> n.getTagNo() == GeneralName.rfc822Name).map(GeneralName::getName)
						.map(IETFUtils::valueToString).toList();

		Stream<String> emails = Stream.concat(Stream.concat(email1.stream(), email2.stream()), rfc822Names.stream());

		return toPractitioner(!surnames.isEmpty() ? surnames.stream() : commonName.stream(), givennames.stream(),
				emails);
	}

	private List<String> getValues(X500Name name, ASN1ObjectIdentifier attribute)
	{
		return Stream.of(name.getRDNs(attribute)).flatMap(rdn -> Stream.of(rdn.getTypesAndValues()))
				.map(AttributeTypeAndValue::getValue).map(IETFUtils::valueToString).toList();
	}
}
