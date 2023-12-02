package dev.dsf.fhir.authorization.process;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Reference;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.common.auth.conf.PractitionerIdentity;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;

public class Organization implements Recipient, Requester
{
	private final String organizationIdentifier;
	private final boolean localIdentity;

	private final String practitionerRoleSystem;
	private final String practitionerRoleCode;

	public Organization(boolean localIdentity, String organizationIdentifier, String practitionerRoleSystem,
			String practitionerRoleCode)
	{
		Objects.requireNonNull(organizationIdentifier, "organizationIdentifier");
		if (organizationIdentifier.isBlank())
			throw new IllegalArgumentException("organizationIdentifier blank");

		this.localIdentity = localIdentity;
		this.organizationIdentifier = organizationIdentifier;

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
				&& identity.isLocalIdentity() == localIdentity && hasOrganizationIdentifier(identity.getOrganization())
				&& ((needsPractitionerRole() && hasPractitionerRole(getPractitionerRoles(identity)))
						|| (!needsPractitionerRole() && identity instanceof OrganizationIdentity));
	}

	private boolean hasOrganizationIdentifier(org.hl7.fhir.r4.model.Organization organization)
	{
		return organization.getIdentifier().stream().filter(Identifier::hasSystem).filter(Identifier::hasValue)
				.filter(i -> ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM.equals(i.getSystem()))
				.anyMatch(i -> organizationIdentifier.equals(i.getValue()));
	}

	private Set<Coding> getPractitionerRoles(Identity identity)
	{
		if (identity instanceof PractitionerIdentity p)
			return p.getPractionerRoles();
		else
			return Collections.emptySet();
	}

	private boolean hasPractitionerRole(Set<Coding> practitionerRoles)
	{
		return practitionerRoles.stream().anyMatch(
				c -> practitionerRoleSystem.equals(c.getSystem()) && practitionerRoleCode.equals(c.getCode()));
	}

	@Override
	public Extension toRecipientExtension()
	{
		return new Extension().setUrl(ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT)
				.setValue(toCoding(false));
	}

	@Override
	public Extension toRequesterExtension()
	{
		return new Extension().setUrl(ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_REQUESTER)
				.setValue(toCoding(needsPractitionerRole()));
	}

	private Coding toCoding(boolean needsPractitionerRole)
	{
		Identifier organization = new Reference().getIdentifier()
				.setSystem(ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM).setValue(organizationIdentifier);

		Coding coding = getProcessAuthorizationCode();

		if (needsPractitionerRole)
		{
			Extension extension = coding.addExtension()
					.setUrl(ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER);
			extension.addExtension(
					ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_ORGANIZATION,
					organization);
			extension.addExtension(
					ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_PRACTITIONER_ROLE,
					new Coding(practitionerRoleSystem, practitionerRoleCode, null));
		}
		else
		{
			coding.addExtension().setUrl(ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION)
					.setValue(organization);
		}

		return coding;
	}

	@Override
	public Coding getProcessAuthorizationCode()
	{
		if (localIdentity)
		{
			if (needsPractitionerRole())
				return new Coding(ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM,
						ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION_PRACTITIONER, null);
			else
				return new Coding(ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM,
						ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION, null);
		}
		else
			return new Coding(ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM,
					ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_REMOTE_ORGANIZATION, null);
	}

	@Override
	public boolean requesterMatches(Extension requesterExtension)
	{
		return matches(requesterExtension, ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_REQUESTER,
				needsPractitionerRole());
	}

	@Override
	public boolean recipientMatches(Extension recipientExtension)
	{
		return matches(recipientExtension, ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT, false);
	}

	private boolean matches(Extension extension, String url, boolean needsPractitionerRole)
	{
		return extension != null && url.equals(extension.getUrl()) && extension.hasValue()
				&& extension.getValue() instanceof Coding value && matches(value) && value.hasExtension()
				&& hasMatchingOrganizationExtension(value.getExtension(), needsPractitionerRole);
	}

	private boolean hasMatchingOrganizationExtension(List<Extension> extensions, boolean needsPractitionerRole)
	{
		return extensions.stream().anyMatch(organizationExtensionMatches(needsPractitionerRole));
	}

	private Predicate<Extension> organizationExtensionMatches(boolean needsPractitionerRole)
	{
		if (needsPractitionerRole)
		{
			return extension -> ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER
					.equals(extension.getUrl()) && !extension.hasValue()
					&& hasMatchingSubOrganizationExtension(extension.getExtension())
					&& hasMatchingPractitionerExtension(extension.getExtension());
		}
		else
		{
			return extension -> ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION
					.equals(extension.getUrl()) && extension.hasValue()
					&& extension.getValue() instanceof Identifier value && organizationIdentifierMatches(value);
		}
	}

	private boolean organizationIdentifierMatches(Identifier identifier)
	{
		return identifier != null && identifier.hasSystem() && identifier.hasValue()
				&& ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM.equals(identifier.getSystem())
				&& organizationIdentifier.equals(identifier.getValue());
	}

	private boolean hasMatchingSubOrganizationExtension(List<Extension> extensions)
	{
		return extensions.stream().anyMatch(this::subOrganizationExtensionMatches);
	}

	private boolean subOrganizationExtensionMatches(Extension extension)
	{
		return ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_ORGANIZATION
				.equals(extension.getUrl()) && extension.hasValue() && extension.getValue() instanceof Identifier value
				&& organizationIdentifierMatches(value);
	}

	private boolean hasMatchingPractitionerExtension(List<Extension> extensions)
	{
		return extensions.stream().anyMatch(this::practitionerExtensionMatches);
	}

	private boolean practitionerExtensionMatches(Extension extension)
	{
		return ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_PRACTITIONER_ROLE
				.equals(extension.getUrl()) && extension.hasValue() && extension.getValue() instanceof Coding value
				&& practitionerRoleMatches(value);
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
				return processAuthorizationCode != null
						&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM
								.equals(processAuthorizationCode.getSystem())
						&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION_PRACTITIONER
								.equals(processAuthorizationCode.getCode());
			else
				return processAuthorizationCode != null
						&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM
								.equals(processAuthorizationCode.getSystem())
						&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION
								.equals(processAuthorizationCode.getCode());
		else
			return processAuthorizationCode != null
					&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM
							.equals(processAuthorizationCode.getSystem())
					&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_REMOTE_ORGANIZATION
							.equals(processAuthorizationCode.getCode());
	}

	public static Optional<Requester> fromRequester(Coding coding, Predicate<Coding> practitionerRoleExists,
			Predicate<Identifier> organizationWithIdentifierExists)
	{
		if (coding != null && coding.hasSystem()
				&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM.equals(coding.getSystem())
				&& coding.hasCode())
		{
			if (ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION.equals(coding.getCode()))
				return from(true, coding, organizationWithIdentifierExists).map(r -> (Requester) r);
			else if (ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_REMOTE_ORGANIZATION
					.equals(coding.getCode()))
				return from(false, coding, organizationWithIdentifierExists).map(r -> (Requester) r);
			else if (ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION_PRACTITIONER
					.equals(coding.getCode()))
				return fromPractitionerRequester(coding, practitionerRoleExists, organizationWithIdentifierExists);
		}

		return Optional.empty();
	}

	public static Optional<Recipient> fromRecipient(Coding coding,
			Predicate<Identifier> organizationWithIdentifierExists)
	{
		if (coding != null && coding.hasSystem()
				&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM.equals(coding.getSystem())
				&& coding.hasCode()
				&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION.equals(coding.getCode()))
		{
			return from(true, coding, organizationWithIdentifierExists).map(r -> (Recipient) r);
		}

		return Optional.empty();
	}

	private static Optional<? super Organization> from(boolean localIdentity, Coding coding,
			Predicate<Identifier> organizationWithIdentifierExists)
	{
		if (coding != null && coding.hasExtension())
		{
			List<Extension> organizations = coding.getExtension().stream().filter(Extension::hasUrl).filter(
					e -> ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION.equals(e.getUrl()))
					.collect(Collectors.toList());
			if (organizations.size() == 1)
			{
				Extension organization = organizations.get(0);
				if (organization.hasValue() && organization.getValue() instanceof Identifier identifier
						&& ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM.equals(identifier.getSystem())
						&& organizationWithIdentifierExists.test(identifier))
				{
					return Optional.of(new Organization(localIdentity, identifier.getValue(), null, null));
				}
			}
		}

		return Optional.empty();
	}

	private static Optional<Requester> fromPractitionerRequester(Coding coding,
			Predicate<Coding> practitionerRoleExists, Predicate<Identifier> organizationWithIdentifierExists)
	{
		if (coding != null && coding.hasExtension())
		{
			List<Extension> organizationPractitioners = coding.getExtension().stream().filter(Extension::hasUrl)
					.filter(e -> ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER
							.equals(e.getUrl()))
					.collect(Collectors.toList());
			if (organizationPractitioners.size() == 1)
			{
				Extension organizationPractitioner = organizationPractitioners.get(0);
				List<Extension> organizations = organizationPractitioner.getExtension().stream()
						.filter(Extension::hasUrl)
						.filter(e -> ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_ORGANIZATION
								.equals(e.getUrl()))
						.collect(Collectors.toList());
				List<Extension> practitionerRoles = organizationPractitioner.getExtension().stream()
						.filter(Extension::hasUrl)
						.filter(e -> ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_PRACTITIONER_ROLE
								.equals(e.getUrl()))
						.collect(Collectors.toList());
				if (organizations.size() == 1 && practitionerRoles.size() == 1)
				{
					Extension organization = organizations.get(0);
					Extension practitionerRole = practitionerRoles.get(0);

					if (organization.hasValue() && organization.getValue() instanceof Identifier organizationIdentifier
							&& practitionerRole.hasValue()
							&& practitionerRole.getValue() instanceof Coding practitionerRoleCoding
							&& ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM
									.equals(organizationIdentifier.getSystem())
							&& organizationWithIdentifierExists.test(organizationIdentifier)
							&& practitionerRoleExists.test(practitionerRoleCoding))
					{
						return Optional.of(new Organization(true, organizationIdentifier.getValue(),
								practitionerRoleCoding.getSystem(), practitionerRoleCoding.getCode()));
					}
				}
			}
		}

		return Optional.empty();
	}
}