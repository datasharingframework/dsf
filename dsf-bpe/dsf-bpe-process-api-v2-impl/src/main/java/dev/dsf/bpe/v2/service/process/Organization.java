package dev.dsf.bpe.v2.service.process;

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

import dev.dsf.bpe.v2.constants.CodeSystems.ProcessAuthorization;
import dev.dsf.bpe.v2.constants.NamingSystems.OrganizationIdentifier;

public class Organization implements Recipient, Requester
{
	private static final String EXTENSION_PROCESS_AUTHORIZATION_REQUESTER = "requester";
	private static final String EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT = "recipient";

	private static final String EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION = "http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-organization";

	private static final String EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER = "http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-organization-practitioner";
	private static final String EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_ORGANIZATION = "organization";
	private static final String EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_PRACTITIONER_ROLE = "practitioner-role";

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
				.filter(i -> OrganizationIdentifier.SID.equals(i.getSystem()))
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
		Identifier organization = OrganizationIdentifier.withValue(organizationIdentifier);
		Coding coding = getProcessAuthorizationCode();

		if (needsPractitionerRole)
		{
			Extension extension = coding.addExtension()
					.setUrl(EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER);
			extension.addExtension(EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_ORGANIZATION,
					organization);
			extension.addExtension(EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_PRACTITIONER_ROLE,
					new Coding(practitionerRoleSystem, practitionerRoleCode, null));
		}
		else
		{
			coding.addExtension().setUrl(EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION).setValue(organization);
		}

		return coding;
	}

	@Override
	public Coding getProcessAuthorizationCode()
	{
		if (localIdentity)
		{
			if (needsPractitionerRole())
				return ProcessAuthorization.localOrganizationPractitioner();
			else
				return ProcessAuthorization.localOrganization();
		}
		else
			return ProcessAuthorization.remoteOrganization();
	}

	@Override
	public boolean requesterMatches(Extension requesterExtension)
	{
		return matches(requesterExtension, EXTENSION_PROCESS_AUTHORIZATION_REQUESTER, needsPractitionerRole());
	}

	@Override
	public boolean recipientMatches(Extension recipientExtension)
	{
		return matches(recipientExtension, EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT, false);
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
			return extension -> EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER.equals(extension.getUrl())
					&& !extension.hasValue() && hasMatchingSubOrganizationExtension(extension.getExtension())
					&& hasMatchingPractitionerExtension(extension.getExtension());
		}
		else
		{
			return extension -> EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION.equals(extension.getUrl())
					&& extension.hasValue() && extension.getValue() instanceof Identifier value
					&& organizationIdentifierMatches(value);
		}
	}

	private boolean organizationIdentifierMatches(Identifier identifier)
	{
		return identifier != null && identifier.hasSystem() && identifier.hasValue()
				&& OrganizationIdentifier.SID.equals(identifier.getSystem())
				&& organizationIdentifier.equals(identifier.getValue());
	}

	private boolean hasMatchingSubOrganizationExtension(List<Extension> extensions)
	{
		return extensions.stream().anyMatch(this::subOrganizationExtensionMatches);
	}

	private boolean subOrganizationExtensionMatches(Extension extension)
	{
		return EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_ORGANIZATION.equals(extension.getUrl())
				&& extension.hasValue() && extension.getValue() instanceof Identifier value
				&& organizationIdentifierMatches(value);
	}

	private boolean hasMatchingPractitionerExtension(List<Extension> extensions)
	{
		return extensions.stream().anyMatch(this::practitionerExtensionMatches);
	}

	private boolean practitionerExtensionMatches(Extension extension)
	{
		return EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_PRACTITIONER_ROLE.equals(extension.getUrl())
				&& extension.hasValue() && extension.getValue() instanceof Coding value
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
				return ProcessAuthorization.isLocalOrganizationPractitioner(processAuthorizationCode);
			else
				return ProcessAuthorization.isLocalOrganization(processAuthorizationCode);
		else
			return ProcessAuthorization.isRemoteOrganization(processAuthorizationCode);
	}

	public static Optional<Requester> fromRequester(Coding coding, Predicate<Coding> practitionerRoleExists,
			Predicate<Identifier> organizationWithIdentifierExists)
	{
		if (ProcessAuthorization.isLocalOrganization(coding))
			return Optional.ofNullable(from(true, coding, organizationWithIdentifierExists));
		else if (ProcessAuthorization.isRemoteOrganization(coding))
			return Optional.ofNullable(from(false, coding, organizationWithIdentifierExists));
		else if (ProcessAuthorization.isLocalOrganizationPractitioner(coding))
			return fromPractitionerRequester(coding, practitionerRoleExists, organizationWithIdentifierExists);
		else
			return Optional.empty();
	}

	public static Optional<Recipient> fromRecipient(Coding coding,
			Predicate<Identifier> organizationWithIdentifierExists)
	{
		if (ProcessAuthorization.isLocalOrganization(coding))
			return Optional.ofNullable(from(true, coding, organizationWithIdentifierExists));
		else
			return Optional.empty();
	}

	private static Organization from(boolean localIdentity, Coding coding,
			Predicate<Identifier> organizationWithIdentifierExists)
	{
		if (coding != null && coding.hasExtension())
		{
			List<Extension> organizations = coding.getExtension().stream().filter(Extension::hasUrl)
					.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION.equals(e.getUrl()))
					.collect(Collectors.toList());
			if (organizations.size() == 1)
			{
				Extension organization = organizations.get(0);
				if (organization.hasValue() && organization.getValue() instanceof Identifier identifier
						&& OrganizationIdentifier.SID.equals(identifier.getSystem())
						&& organizationWithIdentifierExists.test(identifier))
				{
					return new Organization(localIdentity, identifier.getValue(), null, null);
				}
			}
		}

		return null;
	}

	private static Optional<Requester> fromPractitionerRequester(Coding coding,
			Predicate<Coding> practitionerRoleExists, Predicate<Identifier> organizationWithIdentifierExists)
	{
		if (coding != null && coding.hasExtension())
		{
			List<Extension> organizationPractitioners = coding.getExtension().stream().filter(Extension::hasUrl)
					.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER.equals(e.getUrl()))
					.collect(Collectors.toList());
			if (organizationPractitioners.size() == 1)
			{
				Extension organizationPractitioner = organizationPractitioners.get(0);
				List<Extension> organizations = organizationPractitioner.getExtension().stream()
						.filter(Extension::hasUrl)
						.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_ORGANIZATION
								.equals(e.getUrl()))
						.collect(Collectors.toList());
				List<Extension> practitionerRoles = organizationPractitioner.getExtension().stream()
						.filter(Extension::hasUrl)
						.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_PRACTITIONER_ROLE
								.equals(e.getUrl()))
						.collect(Collectors.toList());
				if (organizations.size() == 1 && practitionerRoles.size() == 1)
				{
					Extension organization = organizations.get(0);
					Extension practitionerRole = practitionerRoles.get(0);

					if (organization.hasValue() && organization.getValue() instanceof Identifier organizationIdentifier
							&& practitionerRole.hasValue()
							&& practitionerRole.getValue() instanceof Coding practitionerRoleCoding
							&& OrganizationIdentifier.SID.equals(organizationIdentifier.getSystem())
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