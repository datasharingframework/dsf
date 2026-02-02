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
package dev.dsf.common.auth.logging;

import java.security.Principal;
import java.util.stream.Collectors;

import org.slf4j.MDC;

import dev.dsf.common.auth.DsfOpenIdCredentials;
import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.common.auth.conf.PractitionerIdentity;
import dev.dsf.common.auth.conf.X509CertificateWrapper;

public class CurrentUserMdcLogger extends AbstractUserLogger
{
	public static final String DSF_NAME = "dsf.user.name";
	public static final String DSF_ROLES = "dsf.user.roles";

	public static final String DSF_ORGANIZATION_IDENTIFIER = "dsf.user.organization.identifier";
	public static final String DSF_ORGANIZATION_THUMBPRINT = "dsf.user.organization.thumbprint";
	public static final String DSF_ORGANIZATION_DN = "dsf.user.organization.dn";

	public static final String DSF_ENDPOINT_IDENTIFIER = "dsf.user.endpoint.identifier";

	public static final String DSF_PRACTITIONER_IDENTIFIER = "dsf.user.practitioner.identifier";
	public static final String DSF_PRACTITIONER_THUMBPRINT = "dsf.user.practitioner.thumbprint";
	public static final String DSF_PRACTITIONER_DN = "dsf.user.practitioner.dn";
	public static final String DSF_PRACTITIONER_SUB = "dsf.user.practitioner.sub";
	public static final String DSF_PRACTITIONER_ROLES = "dsf.user.practitioner.roles";


	@Override
	protected void before(OrganizationIdentity organization)
	{
		before((Identity) organization);

		organization.getCertificate().map(X509CertificateWrapper::thumbprint)
				.ifPresent(t -> MDC.put(DSF_ORGANIZATION_THUMBPRINT, t));
		organization.getCertificate().map(X509CertificateWrapper::subjectDn)
				.ifPresent(d -> MDC.put(DSF_ORGANIZATION_DN, d));

		organization.getOrganizationIdentifierValue().ifPresent(i -> MDC.put(DSF_ORGANIZATION_IDENTIFIER, i));
		organization.getEndpointIdentifierValue().ifPresent(i -> MDC.put(DSF_ENDPOINT_IDENTIFIER, i));
	}

	@Override
	protected void before(PractitionerIdentity practitioner)
	{
		before((Identity) practitioner);

		practitioner.getCertificate().map(X509CertificateWrapper::thumbprint)
				.ifPresent(t -> MDC.put(DSF_PRACTITIONER_THUMBPRINT, t));
		practitioner.getCertificate().map(X509CertificateWrapper::subjectDn)
				.ifPresent(d -> MDC.put(DSF_PRACTITIONER_DN, d));
		practitioner.getCredentials().map(DsfOpenIdCredentials::getUserId)
				.ifPresent(i -> MDC.put(DSF_PRACTITIONER_SUB, i));

		practitioner.getOrganizationIdentifierValue().ifPresent(i -> MDC.put(DSF_ORGANIZATION_IDENTIFIER, i));
		practitioner.getEndpointIdentifierValue().ifPresent(i -> MDC.put(DSF_ENDPOINT_IDENTIFIER, i));
		practitioner.getPractitionerIdentifierValue().ifPresent(i -> MDC.put(DSF_PRACTITIONER_IDENTIFIER, i));

		if (!practitioner.getPractionerRoles().isEmpty())
			MDC.put(DSF_PRACTITIONER_ROLES, practitioner.getPractionerRoles().stream()
					.map(c -> c.getSystem() + "|" + c.getCode()).collect(Collectors.joining(", ", "[", "]")));
	}

	private void before(Identity identity)
	{
		if (!identity.getDsfRoles().isEmpty())
			MDC.put(DSF_ROLES,
					identity.getDsfRoles().stream().map(DsfRole::name).collect(Collectors.joining(", ", "[", "]")));
	}

	@Override
	protected void before(Principal principal)
	{
		MDC.put(DSF_NAME, principal.getName());
	}

	@Override
	protected void after()
	{
		MDC.remove(DSF_ROLES);

		MDC.remove(DSF_ORGANIZATION_IDENTIFIER);
		MDC.remove(DSF_ORGANIZATION_THUMBPRINT);
		MDC.remove(DSF_ORGANIZATION_DN);

		MDC.remove(DSF_ENDPOINT_IDENTIFIER);

		MDC.remove(DSF_PRACTITIONER_IDENTIFIER);
		MDC.remove(DSF_PRACTITIONER_THUMBPRINT);
		MDC.remove(DSF_PRACTITIONER_DN);
		MDC.remove(DSF_PRACTITIONER_SUB);
		MDC.remove(DSF_PRACTITIONER_ROLES);

		MDC.remove(DSF_NAME);
	}
}
