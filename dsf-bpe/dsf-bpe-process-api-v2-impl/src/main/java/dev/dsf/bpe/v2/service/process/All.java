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
package dev.dsf.bpe.v2.service.process;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

import dev.dsf.bpe.v2.constants.CodeSystems.ProcessAuthorization;

public class All implements Recipient, Requester
{
	private static final String EXTENSION_PROCESS_AUTHORIZATION_REQUESTER = "requester";
	private static final String EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT = "recipient";

	private static final String EXTENSION_PROCESS_AUTHORIZATION_PRACTITIONER = "http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-practitioner";

	private final boolean localIdentity;

	private final String practitionerRoleSystem;
	private final String practitionerRoleCode;

	public All(boolean localIdentity, String practitionerRoleSystem, String practitionerRoleCode)
	{
		this.localIdentity = localIdentity;

		this.practitionerRoleSystem = practitionerRoleSystem;
		this.practitionerRoleCode = practitionerRoleCode;
	}

	private boolean needsPractitionerRole()
	{
		return practitionerRoleSystem != null && practitionerRoleCode != null;
	}

	@Override
	public boolean isRequesterAuthorized(Identity requester, Stream<OrganizationAffiliation> requesterAffiliations)
	{
		return isAuthorized(requester);
	}

	@Override
	public boolean isRecipientAuthorized(Identity recipient, Stream<OrganizationAffiliation> recipientAffiliations)
	{
		return isAuthorized(recipient);
	}

	private boolean isAuthorized(Identity identity)
	{
		return identity != null && identity.getOrganization() != null && identity.getOrganization().getActive()
				&& identity.isLocalIdentity() == localIdentity
				&& ((needsPractitionerRole() && hasPractitionerRole(getPractitionerRoles(identity)))
						|| (!needsPractitionerRole() && identity instanceof OrganizationIdentity));
	}

	private Set<Coding> getPractitionerRoles(Identity identity)
	{
		if (identity instanceof PractitionerIdentity p)
			return p.getPractionerRoles();
		else
			return Set.of();
	}

	private boolean hasPractitionerRole(Set<Coding> practitionerRoles)
	{
		return practitionerRoles.stream().anyMatch(
				c -> practitionerRoleSystem.equals(c.getSystem()) && practitionerRoleCode.equals(c.getCode()));
	}

	@Override
	public Extension toRecipientExtension()
	{
		return new Extension().setUrl(EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT).setValue(toCoding(false));
	}

	@Override
	public Extension toRequesterExtension()
	{
		return new Extension().setUrl(EXTENSION_PROCESS_AUTHORIZATION_REQUESTER)
				.setValue(toCoding(needsPractitionerRole()));
	}

	private Coding toCoding(boolean needsPractitionerRole)
	{
		Coding coding = getProcessAuthorizationCode();

		if (needsPractitionerRole)
			coding.addExtension().setUrl(EXTENSION_PROCESS_AUTHORIZATION_PRACTITIONER)
					.setValue(new Coding(practitionerRoleSystem, practitionerRoleCode, null));

		return coding;
	}

	@Override
	public Coding getProcessAuthorizationCode()
	{
		if (localIdentity)
		{
			if (needsPractitionerRole())
				return ProcessAuthorization.localAllPractitioner();
			else
				return ProcessAuthorization.localAll();
		}
		else
			return ProcessAuthorization.remoteAll();
	}

	@Override
	public boolean requesterMatches(Extension requesterExtension)
	{
		return matches(requesterExtension, EXTENSION_PROCESS_AUTHORIZATION_REQUESTER)
				&& hasMatchingPractitionerExtension(requesterExtension.getValue().getExtension());
	}

	@Override
	public boolean recipientMatches(Extension recipientExtension)
	{
		return matches(recipientExtension, EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT);
	}

	private boolean matches(Extension extension, String url)
	{
		return extension != null && url.equals(extension.getUrl()) && extension.hasValue()
				&& extension.getValue() instanceof Coding value && matches(value);
	}

	private boolean hasMatchingPractitionerExtension(List<Extension> extensions)
	{
		return needsPractitionerRole() ? extensions.stream().anyMatch(this::practitionerExtensionMatches)
				: extensions.stream().noneMatch(this::practitionerExtensionMatches);
	}

	private boolean practitionerExtensionMatches(Extension extension)
	{
		return EXTENSION_PROCESS_AUTHORIZATION_PRACTITIONER.equals(extension.getUrl()) && extension.hasValue()
				&& extension.getValue() instanceof Coding value && practitionerRoleMatches(value);
	}

	private boolean practitionerRoleMatches(Coding coding)
	{
		return coding != null && coding.hasSystem() && coding.hasCode()
				&& practitionerRoleSystem.equals(coding.getSystem()) && practitionerRoleCode.equals(coding.getCode());
	}

	@Override
	public boolean matches(Coding processAuthorizationCode)
	{
		if (localIdentity)
			if (needsPractitionerRole())
				return ProcessAuthorization.isLocalAllPractitioner(processAuthorizationCode);
			else
				return ProcessAuthorization.isLocalAll(processAuthorizationCode);
		else
			return ProcessAuthorization.isRemoteAll(processAuthorizationCode);
	}

	public static Optional<Requester> fromRequester(Coding coding, Predicate<Coding> practitionerRoleExists)
	{
		if (ProcessAuthorization.isLocalAll(coding))
			return Optional.of(new All(true, null, null));
		else if (ProcessAuthorization.isRemoteAll(coding))
			return Optional.of(new All(false, null, null));
		else if (ProcessAuthorization.isLocalAllPractitioner(coding))
			return fromPractitionerRequester(coding, practitionerRoleExists);
		else
			return Optional.empty();
	}

	private static Optional<Requester> fromPractitionerRequester(Coding coding,
			Predicate<Coding> practitionerRoleExists)
	{
		if (coding != null && coding.hasExtension())
		{
			List<Extension> practitionerRoles = coding.getExtension().stream().filter(Extension::hasUrl)
					.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_PRACTITIONER.equals(e.getUrl()))
					.collect(Collectors.toList());
			if (practitionerRoles.size() == 1)
			{
				Extension practitionerRole = practitionerRoles.get(0);
				if (practitionerRole.hasValue() && practitionerRole.getValue() instanceof Coding value
						&& value.hasSystem() && value.hasCode() && practitionerRoleExists.test(coding))
				{
					return Optional.of(new All(true, value.getSystem(), value.getCode()));
				}
			}
		}

		return Optional.empty();
	}

	public static Optional<Recipient> fromRecipient(Coding coding)
	{
		if (ProcessAuthorization.isLocalAll(coding))
			return Optional.of(new All(true, null, null));
		else
			// remote not allowed for recipient
			return Optional.empty();
	}
}