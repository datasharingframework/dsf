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
package dev.dsf.bpe.authentication;

import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Optional;

import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Practitioner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.service.LocalOrganizationAndEndpointProvider;
import dev.dsf.common.auth.conf.AbstractIdentityProvider;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.IdentityProvider;
import dev.dsf.common.auth.conf.PractitionerIdentityImpl;
import dev.dsf.common.auth.conf.RoleConfig;
import dev.dsf.common.auth.conf.X509CertificateWrapper;

public class IdentityProviderImpl extends AbstractIdentityProvider<BpeServerRole>
		implements IdentityProvider, InitializingBean
{
	private static final Logger logger = LoggerFactory.getLogger(IdentityProviderImpl.class);

	private final LocalOrganizationAndEndpointProvider organizationAndEndpointProvider;

	public IdentityProviderImpl(RoleConfig<BpeServerRole> roleConfig,
			LocalOrganizationAndEndpointProvider organizationAndEndpointProvider)
	{
		super(roleConfig);

		this.organizationAndEndpointProvider = organizationAndEndpointProvider;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(organizationAndEndpointProvider, "organizationAndEndpointProvider");
	}

	@Override
	protected Optional<Organization> getLocalOrganization()
	{
		return organizationAndEndpointProvider.getLocalOrganization();
	}

	@Override
	public Identity getIdentity(X509Certificate[] certificates)
	{
		if (certificates == null || certificates.length == 0)
			return null;

		X509CertificateWrapper certWrapper = new X509CertificateWrapper(certificates[0]);

		Optional<Practitioner> practitioner = toPractitioner(certWrapper.certificate());
		Optional<Organization> localOrganization = organizationAndEndpointProvider.getLocalOrganization();
		Optional<Endpoint> localEndpoint = organizationAndEndpointProvider.getLocalEndpoint();
		if (practitioner.isPresent() && localOrganization.isPresent() && localEndpoint.isPresent())
		{
			Practitioner p = practitioner.get();
			Organization o = localOrganization.get();
			Endpoint e = localEndpoint.get();

			return new PractitionerIdentityImpl(o, e, getDsfRolesFor(p, certWrapper.thumbprint(), null, null),
					certWrapper, p, getPractitionerRolesFor(p, certWrapper.thumbprint(), null, null), null);
		}
		else
		{
			logger.warn(
					"Certificate with thumbprint '{}' for '{}' unknown, not configured as local user or local organization unknown",
					certWrapper.thumbprint(), certWrapper.subjectDn());
			return null;
		}
	}

	@Override
	protected Optional<Endpoint> getLocalEndpoint()
	{
		return Optional.empty();
	}
}
