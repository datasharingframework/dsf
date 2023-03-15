package dev.dsf.fhir.authentication;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jetty.security.openid.JwtDecoder;
import org.eclipse.jetty.security.openid.OpenIdCredentials;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.IdentityProvider;
import dev.dsf.common.auth.conf.Role;
import dev.dsf.common.auth.conf.RoleConfig;

public class IdentityProviderImpl extends AbstractProvider implements IdentityProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(IdentityProviderImpl.class);

	private static final String ACCESS_TOKEN = "access_token";
	private static final String ID_TOKEN = "id_token";

	private final OrganizationProvider organizationProvider;
	private final PractitionerProvider practitionerProvider;
	private final String localOrganizationIdentifierValue;
	private final RoleConfig roleConfig;

	public IdentityProviderImpl(OrganizationProvider organizationProvider, PractitionerProvider practitionerProvider,
			String localOrganizationIdentifierValue, RoleConfig roleConfig)
	{
		this.organizationProvider = organizationProvider;
		this.practitionerProvider = practitionerProvider;
		this.localOrganizationIdentifierValue = localOrganizationIdentifierValue;
		this.roleConfig = roleConfig;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(practitionerProvider, "practitionerProvider");
		Objects.requireNonNull(localOrganizationIdentifierValue, "localOrganizationIdentifierValue");
		Objects.requireNonNull(roleConfig, "roleConfig");
	}

	@Override
	public Identity getIdentity(OpenIdCredentials credentials)
	{
		Optional<Organization> localOrganization = organizationProvider.getLocalOrganization();
		Optional<Practitioner> practitioner = practitionerProvider.getPractitioner(credentials);

		if (practitioner.isPresent() && localOrganization.isPresent())
		{
			Map<String, Object> parsedIdToken = getToken(credentials, ID_TOKEN);
			Map<String, Object> parsedAccessToken = getToken(credentials, ACCESS_TOKEN);

			return new PractitionerIdentityImpl(localOrganization.get(),
					getRolesFor(practitioner.get(), null, getRolesFromTokens(parsedIdToken, parsedAccessToken),
							getGroupsFromTokens(parsedIdToken, parsedAccessToken)),
					practitioner.get(), null, credentials);
		}

		logger.warn("User from OpenID Connect token '{}' not configured as local user or local organization unknown",
				credentials.getUserId());
		return null;
	}

	private Map<String, Object> getToken(OpenIdCredentials credentials, String tokenName)
	{
		String token = (String) credentials.getResponse().get(tokenName);

		logger.debug("{}: {}", tokenName, token);

		return JwtDecoder.decode(token);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getPropertyMap(Map<String, Object> map, String property)
	{
		Object propertyValue = map.get(property);
		if (propertyValue != null && propertyValue instanceof Map)
			return (Map<String, Object>) propertyValue;
		else
			return Collections.emptyMap();
	}

	private List<String> getPropertyArray(Map<String, Object> map, String property)
	{
		Object propertyValue = map.get(property);
		if (propertyValue != null && propertyValue instanceof Object[])
			return Arrays.stream((Object[]) propertyValue).filter(v -> v instanceof String).map(v -> (String) v)
					.toList();
		else
			return Collections.emptyList();
	}

	private Stream<String> getRolesFromTokens(Map<String, Object> idToken, Map<String, Object> accessToken)
	{
		return Stream.concat(getRolesFromToken(ID_TOKEN, idToken), getRolesFromToken(ACCESS_TOKEN, accessToken));
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

	private Stream<String> getGroupsFromTokens(Map<String, Object> parsedIdToken, Map<String, Object> parsedAccessToken)
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("{}: groups: {}", ID_TOKEN, getPropertyArray(parsedIdToken, "groups"));
			logger.debug("{}: groups: {}", ACCESS_TOKEN, getPropertyArray(parsedAccessToken, "groups"));
		}

		return Stream.concat(getPropertyArray(parsedIdToken, "groups").stream(),
				getPropertyArray(parsedAccessToken, "groups").stream());
	}

	@Override
	public Identity getIdentity(X509Certificate[] certificates)
	{
		if (certificates == null || certificates.length == 0)
			return null;

		String thumbprint = getThumbprint(certificates[0]);

		Optional<Organization> organization = organizationProvider.getOrganization(certificates[0]);
		if (organization.isPresent())
		{
			boolean localOrganization = isLocalOrganization(organization.get());

			if (localOrganization)
				return new OrganizationIdentityImpl(true, organization.get(), FhirServerRole.LOCAL_ORGANIZATION,
						certificates[0]);
			else
				return new OrganizationIdentityImpl(false, organization.get(), FhirServerRole.REMOTE_ORGANIZATION,
						certificates[0]);
		}

		Optional<Practitioner> practitioner = practitionerProvider.getPractitioner(certificates[0]);
		Optional<Organization> localOrganization = organizationProvider.getLocalOrganization();
		if (practitioner.isPresent() && localOrganization.isPresent())
			return new PractitionerIdentityImpl(localOrganization.get(),
					getRolesFor(practitioner.get(), thumbprint, null, null), practitioner.get(), certificates[0], null);

		logger.warn(
				"Certificate with thumbprint '{}' for '{}' not part of allowlist and not configured as local user or local organization unknown",
				thumbprint, getDn(certificates[0]));
		return null;
	}

	private boolean isLocalOrganization(Organization organization)
	{
		return organization != null && organization.getIdentifier().stream()
				.anyMatch(i -> i != null && OrganizationProvider.ORGANIZATION_IDENTIFIER_SYSTEM.equals(i.getSystem())
						&& localOrganizationIdentifierValue.equals(i.getValue()));
	}

	// thumbprint from certificate, token roles and groups from jwt
	private Set<Role> getRolesFor(Practitioner practitioner, String thumbprint, Stream<String> tokenRoles,
			Stream<String> tokenGroups)
	{
		Stream<String> emailAddresses = practitioner.getIdentifier().stream()
				.filter(i -> PractitionerProvider.PRACTITIONER_IDENTIFIER_SYSTEM.equals(i.getSystem()) && i.hasValue())
				.map(Identifier::getValue);

		Stream<Role> r1 = emailAddresses.flatMap(e -> roleConfig.getRolesForEmail(e).stream());
		Stream<Role> r2 = thumbprint == null ? Stream.empty() : roleConfig.getRolesForThumbprint(thumbprint).stream();
		Stream<Role> r3 = tokenRoles == null ? Stream.empty()
				: tokenRoles.flatMap(c -> roleConfig.getRolesForTokenRole(c).stream());
		Stream<Role> r4 = tokenGroups == null ? Stream.empty()
				: tokenGroups.flatMap(c -> roleConfig.getRolesForTokenGroup(c).stream());

		return Stream.of(r1, r2, r3, r4).flatMap(Function.identity()).distinct().collect(Collectors.toSet());
	}
}
