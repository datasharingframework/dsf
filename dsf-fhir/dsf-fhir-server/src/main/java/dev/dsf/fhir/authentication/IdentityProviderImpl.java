/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.fhir.authentication;

import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.auth.conf.AbstractIdentityProvider;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.IdentityProvider;
import dev.dsf.common.auth.conf.OrganizationIdentityImpl;
import dev.dsf.common.auth.conf.PractitionerIdentityImpl;
import dev.dsf.common.auth.conf.RoleConfig;
import dev.dsf.common.auth.conf.X509CertificateWrapper;

public class IdentityProviderImpl extends AbstractIdentityProvider<FhirServerRole>
		implements IdentityProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(IdentityProviderImpl.class);

	private final OrganizationProvider organizationProvider;
	private final EndpointProvider endpointProvider;
	private final String localOrganizationIdentifierValue;

	public IdentityProviderImpl(RoleConfig<FhirServerRole> roleConfig, OrganizationProvider organizationProvider,
			EndpointProvider endpointProvider, String localOrganizationIdentifierValue)
	{
		super(roleConfig);

		this.organizationProvider = organizationProvider;
		this.endpointProvider = endpointProvider;
		this.localOrganizationIdentifierValue = localOrganizationIdentifierValue;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(organizationProvider, "organizationProvider");
		Objects.requireNonNull(endpointProvider, "endpointProvider");
		Objects.requireNonNull(localOrganizationIdentifierValue, "localOrganizationIdentifierValue");
	}

	@Override
	protected Optional<Organization> getLocalOrganization()
	{
		return organizationProvider.getLocalOrganization();
	}

	@Override
	protected Optional<Endpoint> getLocalEndpoint()
	{
		return endpointProvider.getLocalEndpoint();
	}

	@Override
	public Identity getIdentity(X509Certificate[] certificates)
	{
		if (certificates == null || certificates.length == 0)
			return null;

		X509CertificateWrapper certWrapper = new X509CertificateWrapper(certificates[0]);

		Optional<Organization> organization = organizationProvider.getOrganization(certWrapper.thumbprint());
		if (organization.isPresent())
		{
			Organization o = organization.get();

			boolean local = isLocalOrganization(o);

			Optional<Endpoint> e = local ? getLocalEndpoint()
					: endpointProvider.getEndpoint(o, certWrapper.thumbprint());
			Set<FhirServerRole> r = local ? FhirServerRoleImpl.LOCAL_ORGANIZATION
					: FhirServerRoleImpl.REMOTE_ORGANIZATION;

			return new OrganizationIdentityImpl(local, o, e.orElse(null), r, certWrapper);
		}

		Optional<Practitioner> practitioner = toPractitioner(certWrapper);
		Optional<Organization> localOrganization = getLocalOrganization();
		if (practitioner.isPresent() && localOrganization.isPresent())
		{
			Practitioner p = practitioner.get();
			Organization o = localOrganization.get();
			Endpoint e = getLocalEndpoint().orElse(null);

			return new PractitionerIdentityImpl(o, e, getDsfRolesFor(p, certWrapper.thumbprint(), null, null),
					certWrapper, p, getPractitionerRolesFor(p, certWrapper.thumbprint(), null, null), null);
		}
		else
		{
			logger.warn(
					"Certificate with thumbprint '{}' for '{}' unknown, not part of allowlist and not configured as local user or local organization",
					certWrapper.thumbprint(), certWrapper.subjectDn());
			return null;
		}
	}

	private boolean isLocalOrganization(Organization organization)
	{
		return organization != null && organization.getIdentifier().stream().filter(i -> i != null)
				.filter(i -> OrganizationProvider.ORGANIZATION_IDENTIFIER_SYSTEM.equals(i.getSystem()))
				.anyMatch(i -> localOrganizationIdentifierValue.equals(i.getValue()));
	}
}
