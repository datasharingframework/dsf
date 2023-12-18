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

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.auth.DsfOpenIdCredentials;
import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.IdentityProvider;
import dev.dsf.common.auth.conf.RoleConfig;

public class IdentityProviderImpl extends AbstractProvider implements IdentityProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(IdentityProviderImpl.class);

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
	public Identity getIdentity(DsfOpenIdCredentials credentials)
	{
		if (credentials == null)
			return null;

		Optional<Practitioner> practitioner = practitionerProvider.getPractitioner(credentials);
		Optional<Organization> localOrganization = organizationProvider.getLocalOrganization();

		if (practitioner.isPresent() && localOrganization.isPresent())
		{
			Map<String, Object> parsedIdToken = credentials.getIdToken();
			Map<String, Object> parsedAccessToken = credentials.getAccessToken();

			List<String> rolesFromTokens = getRolesFromTokens(parsedIdToken, parsedAccessToken);
			List<String> groupsFromTokens = getGroupsFromTokens(parsedIdToken, parsedAccessToken);

			Set<DsfRole> dsfRoles = getDsfRolesFor(practitioner.get(), null, rolesFromTokens, groupsFromTokens);
			Set<Coding> practitionerRoles = getPractitionerRolesFor(practitioner.get(), null, rolesFromTokens,
					groupsFromTokens);

			return new PractitionerIdentityImpl(localOrganization.get(), dsfRoles, null, practitioner.get(),
					practitionerRoles, credentials);
		}
		else
		{
			logger.warn(
					"User from OpenID Connect token '{}' not configured as local user or local organization unknown",
					credentials.getUserId());
			return null;
		}
	}

	private List<String> getGroupsFromTokens(Map<String, Object> parsedIdToken, Map<String, Object> parsedAccessToken)
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("id_token: groups: {}", getPropertyArray(parsedIdToken, "groups"));
			logger.debug("access_token: groups: {}", getPropertyArray(parsedAccessToken, "groups"));
		}

		return Stream.concat(getPropertyArray(parsedIdToken, "groups").stream(),
				getPropertyArray(parsedAccessToken, "groups").stream()).toList();
	}

	private List<String> getRolesFromTokens(Map<String, Object> idToken, Map<String, Object> accessToken)
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
			return Collections.emptyMap();
	}

	private List<String> getPropertyArray(Map<String, Object> map, String property)
	{
		Object propertyValue = map.get(property);
		if (propertyValue != null && propertyValue instanceof Object[] o)
			return Arrays.stream(o).filter(v -> v instanceof String).map(v -> (String) v).toList();
		else
			return Collections.emptyList();
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
		{
			Practitioner p = practitioner.get();
			Organization o = localOrganization.get();

			return new PractitionerIdentityImpl(o, getDsfRolesFor(p, thumbprint, null, null), certificates[0], p,
					getPractitionerRolesFor(p, thumbprint, null, null), null);
		}
		else
		{
			logger.warn(
					"Certificate with thumbprint '{}' for '{}' unknown, not part of allowlist and not configured as local user or local organization",
					thumbprint, getDn(certificates[0]));
			return null;
		}
	}

	private boolean isLocalOrganization(Organization organization)
	{
		return organization != null && organization.getIdentifier().stream().filter(i -> i != null)
				.filter(i -> OrganizationProvider.ORGANIZATION_IDENTIFIER_SYSTEM.equals(i.getSystem()))
				.anyMatch(i -> localOrganizationIdentifierValue.equals(i.getValue()));
	}

	// thumbprint from certificate, token roles and groups from jwt
	private Set<DsfRole> getDsfRolesFor(Practitioner practitioner, String thumbprint, List<String> tokenRoles,
			List<String> tokenGroups)
	{
		Stream<String> emailAddresses = practitioner.getIdentifier().stream()
				.filter(i -> PractitionerProvider.PRACTITIONER_IDENTIFIER_SYSTEM.equals(i.getSystem()) && i.hasValue())
				.map(Identifier::getValue);

		Stream<DsfRole> r1 = emailAddresses.map(roleConfig::getDsfRolesForEmail).flatMap(List::stream);
		Stream<DsfRole> r2 = thumbprint == null ? Stream.empty()
				: roleConfig.getDsfRolesForThumbprint(thumbprint).stream();
		Stream<DsfRole> r3 = tokenRoles == null ? Stream.empty()
				: tokenRoles.stream().map(roleConfig::getDsfRolesForTokenRole).flatMap(List::stream);
		Stream<DsfRole> r4 = tokenGroups == null ? Stream.empty()
				: tokenGroups.stream().map(roleConfig::getDsfRolesForTokenGroup).flatMap(List::stream);

		return Stream.of(r1, r2, r3, r4).flatMap(Function.identity()).distinct().collect(Collectors.toSet());
	}

	// thumbprint from certificate, token roles and groups from jwt
	private Set<Coding> getPractitionerRolesFor(Practitioner practitioner, String thumbprint, List<String> tokenRoles,
			List<String> tokenGroups)
	{
		Stream<String> emailAddresses = practitioner.getIdentifier().stream()
				.filter(i -> PractitionerProvider.PRACTITIONER_IDENTIFIER_SYSTEM.equals(i.getSystem()) && i.hasValue())
				.map(Identifier::getValue);

		Stream<Coding> r1 = emailAddresses.map(roleConfig::getPractitionerRolesForEmail).flatMap(List::stream);
		Stream<Coding> r2 = thumbprint == null ? Stream.empty()
				: roleConfig.getPractitionerRolesForThumbprint(thumbprint).stream();
		Stream<Coding> r3 = tokenRoles == null ? Stream.empty()
				: tokenRoles.stream().map(roleConfig::getPractitionerRolesForTokenRole).flatMap(List::stream);
		Stream<Coding> r4 = tokenGroups == null ? Stream.empty()
				: tokenGroups.stream().map(roleConfig::getPractitionerRolesForTokenGroup).flatMap(List::stream);

		return Stream.of(r1, r2, r3, r4).flatMap(Function.identity()).distinct().collect(Collectors.toSet());
	}
}
