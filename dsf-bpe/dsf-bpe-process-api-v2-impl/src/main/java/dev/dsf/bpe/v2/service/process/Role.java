package dev.dsf.bpe.v2.service.process;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

import dev.dsf.bpe.v2.constants.CodeSystems.ProcessAuthorization;
import dev.dsf.bpe.v2.constants.NamingSystems.OrganizationIdentifier;

public class Role implements Recipient, Requester
{
	private static final String EXTENSION_PROCESS_AUTHORIZATION_REQUESTER = "requester";
	private static final String EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT = "recipient";

	private static final String EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE = "http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-parent-organization-role";
	private static final String EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PARENT_ORGANIZATION = "parent-organization";
	private static final String EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_ORGANIZATION_ROLE = "organization-role";

	private static final String EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER = "http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-parent-organization-role-practitioner";
	private static final String EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER_PRACTITIONER_ROLE = "practitioner-role";

	private final boolean localIdentity;
	private final String parentOrganizationIdentifier;
	private final String organizationRoleSystem;
	private final String organizationRoleCode;

	private final String practitionerRoleSystem;
	private final String practitionerRoleCode;

	public Role(boolean localIdentity, String parentOrganizationIdentifier, String organizatioRoleSystem,
			String organizationRoleCode, String practitionerRoleSystem, String practitionerRoleCode)
	{
		Objects.requireNonNull(parentOrganizationIdentifier, "parentOrganizationIdentifier");
		if (parentOrganizationIdentifier.isBlank())
			throw new IllegalArgumentException("parentOrganizationIdentifier blank");
		Objects.requireNonNull(organizatioRoleSystem, "organizatioRoleSystem");
		if (organizatioRoleSystem.isBlank())
			throw new IllegalArgumentException("organizatioRoleSystem blank");
		Objects.requireNonNull(organizationRoleCode, "organizationRoleCode");
		if (organizationRoleCode.isBlank())
			throw new IllegalArgumentException("organizationRoleCode blank");

		this.localIdentity = localIdentity;
		this.parentOrganizationIdentifier = parentOrganizationIdentifier;
		this.organizationRoleSystem = organizatioRoleSystem;
		this.organizationRoleCode = organizationRoleCode;

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
		return isAuthorized(requester, requesterAffiliations);
	}

	@Override
	public boolean isRecipientAuthorized(Identity recipient, Stream<OrganizationAffiliation> recipientAffiliations)
	{
		return isAuthorized(recipient, recipientAffiliations);
	}

	private boolean isAuthorized(Identity identity, Stream<OrganizationAffiliation> affiliations)
	{
		return identity != null && identity.getOrganization() != null && identity.getOrganization().getActive()
				&& identity.isLocalIdentity() == localIdentity && affiliations != null
				&& hasParentOrganizationMemberRole(identity.getOrganization(), affiliations)
				&& ((needsPractitionerRole() && hasPractitionerRole(getPractitionerRoles(identity)))
						|| (!needsPractitionerRole() && identity instanceof OrganizationIdentity));
	}

	private boolean hasParentOrganizationMemberRole(org.hl7.fhir.r4.model.Organization recipientOrganization,
			Stream<OrganizationAffiliation> affiliations)
	{
		return affiliations

				// check affiliation active
				.filter(OrganizationAffiliation::getActive)

				// check parent-organization identifier
				.filter(OrganizationAffiliation::hasOrganization).filter(a -> a.getOrganization().hasIdentifier())
				.filter(a -> a.getOrganization().getIdentifier().hasSystem())
				.filter(a -> a.getOrganization().getIdentifier().hasValue())
				.filter(a -> OrganizationIdentifier.SID.equals(a.getOrganization().getIdentifier().getSystem()))
				.filter(a -> parentOrganizationIdentifier.equals(a.getOrganization().getIdentifier().getValue()))

				// check member identifier
				.filter(OrganizationAffiliation::hasParticipatingOrganization)
				.filter(a -> a.getParticipatingOrganization().hasIdentifier())
				.filter(a -> a.getParticipatingOrganization().getIdentifier().hasSystem())
				.filter(a -> a.getParticipatingOrganization().getIdentifier().hasValue()).filter(a ->
				{
					final Identifier memberIdentifier = a.getParticipatingOrganization().getIdentifier();
					return recipientOrganization.getIdentifier().stream().filter(Identifier::hasSystem)
							.filter(Identifier::hasValue)
							.anyMatch(i -> i.getSystem().equals(memberIdentifier.getSystem())
									&& i.getValue().equals(memberIdentifier.getValue()));
				})

				// check role
				.filter(OrganizationAffiliation::hasCode).flatMap(a -> a.getCode().stream())
				.filter(CodeableConcept::hasCoding).flatMap(c -> c.getCoding().stream()).filter(Coding::hasSystem)
				.filter(Coding::hasCode).anyMatch(
						c -> c.getSystem().equals(organizationRoleSystem) && c.getCode().equals(organizationRoleCode));
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
		Identifier parentOrganization = OrganizationIdentifier.withValue(parentOrganizationIdentifier);
		Extension parentOrganizationExt = new Extension(
				EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PARENT_ORGANIZATION, parentOrganization);

		Coding organizationRole = new Coding(organizationRoleSystem, organizationRoleCode, null);
		Extension organizationRoleExt = new Extension(
				EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_ORGANIZATION_ROLE, organizationRole);

		Coding coding = getProcessAuthorizationCode();

		if (needsPractitionerRole)
		{
			Extension practitionerRoleExt = new Extension(
					EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER_PRACTITIONER_ROLE,
					new Coding(practitionerRoleSystem, practitionerRoleCode, null));

			coding.addExtension().setUrl(EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER)
					.addExtension(parentOrganizationExt).addExtension(organizationRoleExt)
					.addExtension(practitionerRoleExt);
		}
		else
		{
			coding.addExtension().setUrl(EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE)
					.addExtension(parentOrganizationExt).addExtension(organizationRoleExt);
		}

		return coding;
	}

	@Override
	public Coding getProcessAuthorizationCode()
	{
		if (localIdentity)
		{
			if (needsPractitionerRole())
				return ProcessAuthorization.localRolePractitioner();
			else
				return ProcessAuthorization.localRole();
		}
		else
			return ProcessAuthorization.remoteRole();
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
				&& hasMatchingParentOrganizationRoleExtension(value.getExtension(), needsPractitionerRole);
	}

	private boolean hasMatchingParentOrganizationRoleExtension(List<Extension> extension, boolean needsPractitionerRole)
	{
		return extension.stream().anyMatch(parentOrganizationRoleExtensionMatches(needsPractitionerRole));
	}

	private Predicate<Extension> parentOrganizationRoleExtensionMatches(boolean needsPractitionerRole)
	{
		if (needsPractitionerRole)
		{
			return extension -> EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER
					.equals(extension.getUrl()) && extension.hasExtension()
					&& hasMatchingParentOrganizationExtension(extension.getExtension())
					&& hasMatchingOrganizationRoleExtension(extension.getExtension())
					&& hasMatchingPractitionerRoleExtension(extension.getExtension());
		}
		else
		{
			return extension -> EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE.equals(extension.getUrl())
					&& extension.hasExtension() && hasMatchingParentOrganizationExtension(extension.getExtension())
					&& hasMatchingOrganizationRoleExtension(extension.getExtension());
		}
	}

	private boolean hasMatchingParentOrganizationExtension(List<Extension> extensions)
	{
		return extensions.stream().anyMatch(this::parentOrganizationExtensionMatches);
	}

	private boolean parentOrganizationExtensionMatches(Extension extension)
	{
		return EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PARENT_ORGANIZATION.equals(extension.getUrl())
				&& extension.hasValue() && extension.getValue() instanceof Identifier value
				&& parentOrganizationIdentifierMatches(value);
	}

	private boolean parentOrganizationIdentifierMatches(Identifier identifier)
	{
		return identifier != null && identifier.hasSystem() && identifier.hasValue()
				&& OrganizationIdentifier.SID.equals(identifier.getSystem())
				&& parentOrganizationIdentifier.equals(identifier.getValue());
	}

	private boolean hasMatchingOrganizationRoleExtension(List<Extension> extensions)
	{
		return extensions.stream().anyMatch(this::organizationRoleExtensionMatches);
	}

	private boolean organizationRoleExtensionMatches(Extension extension)
	{
		return EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_ORGANIZATION_ROLE.equals(extension.getUrl())
				&& extension.hasValue() && extension.getValue() instanceof Coding value
				&& organizationRoleMatches(value);
	}

	private boolean organizationRoleMatches(Coding coding)
	{
		return coding != null && coding.hasSystem() && coding.hasCode()
				&& organizationRoleSystem.equals(coding.getSystem()) && organizationRoleCode.equals(coding.getCode());
	}

	private boolean hasMatchingPractitionerRoleExtension(List<Extension> extensions)
	{
		return extensions.stream().anyMatch(this::practitionerRoleExtensionMatches);
	}

	private boolean practitionerRoleExtensionMatches(Extension extension)
	{
		return EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER_PRACTITIONER_ROLE
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
				return ProcessAuthorization.isLocalRolePractitioner(processAuthorizationCode);
			else
				return ProcessAuthorization.isLocalRole(processAuthorizationCode);
		else
			return ProcessAuthorization.isRemoteRole(processAuthorizationCode);
	}

	public static Optional<Requester> fromRequester(Coding coding, Predicate<Coding> practitionerRoleExists,
			Predicate<Identifier> organizationWithIdentifierExists, Predicate<Coding> organizationRoleExists)
	{
		if (ProcessAuthorization.isLocalRole(coding))
			return Optional.ofNullable(from(true, coding, organizationWithIdentifierExists, organizationRoleExists));
		else if (ProcessAuthorization.isRemoteRole(coding))
			return Optional.ofNullable(from(false, coding, organizationWithIdentifierExists, organizationRoleExists));
		else if (ProcessAuthorization.isLocalRolePractitioner(coding))
			return fromPractitionerRequester(coding, practitionerRoleExists, organizationWithIdentifierExists,
					organizationRoleExists);
		else
			return Optional.empty();
	}

	public static Optional<Recipient> fromRecipient(Coding coding,
			Predicate<Identifier> organizationWithIdentifierExists, Predicate<Coding> organizationRoleExists)
	{
		if (ProcessAuthorization.isLocalRole(coding))
			return Optional.ofNullable(from(true, coding, organizationWithIdentifierExists, organizationRoleExists));
		else
			return Optional.empty();
	}

	private static Role from(boolean localIdentity, Coding coding,
			Predicate<Identifier> organizationWithIdentifierExists, Predicate<Coding> organizationRoleExists)
	{
		if (coding != null && coding.hasExtension())
		{
			List<Extension> parentOrganizationRoles = coding.getExtension().stream().filter(Extension::hasUrl)
					.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE.equals(e.getUrl()))
					.collect(Collectors.toList());

			if (parentOrganizationRoles.size() == 1)
			{
				Extension parentOrganizationRole = parentOrganizationRoles.get(0);
				List<Extension> parentOrganizations = parentOrganizationRole.getExtension().stream()
						.filter(Extension::hasUrl)
						.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PARENT_ORGANIZATION
								.equals(e.getUrl()))
						.collect(Collectors.toList());
				List<Extension> organizationRoles = parentOrganizationRole.getExtension().stream()
						.filter(Extension::hasUrl)
						.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_ORGANIZATION_ROLE
								.equals(e.getUrl()))
						.collect(Collectors.toList());

				if (parentOrganizations.size() == 1 && organizationRoles.size() == 1)
				{
					Extension parentOrganization = parentOrganizations.get(0);
					Extension organizationRole = organizationRoles.get(0);

					if (parentOrganization.hasValue()
							&& parentOrganization.getValue() instanceof Identifier parentOrganizationIdentifier
							&& organizationRole.hasValue()
							&& organizationRole.getValue() instanceof Coding organizationRoleCoding
							&& OrganizationIdentifier.SID.equals(parentOrganizationIdentifier.getSystem())
							&& organizationWithIdentifierExists.test(parentOrganizationIdentifier)
							&& organizationRoleExists.test(organizationRoleCoding))
					{
						return new Role(localIdentity, parentOrganizationIdentifier.getValue(),
								organizationRoleCoding.getSystem(), organizationRoleCoding.getCode(), null, null);
					}
				}
			}
		}

		return null;
	}

	private static Optional<Requester> fromPractitionerRequester(Coding coding,
			Predicate<Coding> practitionerRoleExists, Predicate<Identifier> organizationWithIdentifierExists,
			Predicate<Coding> organizationRoleExists)
	{
		if (coding != null && coding.hasExtension())
		{
			List<Extension> parentOrganizationRolePractitioners = coding.getExtension().stream()
					.filter(Extension::hasUrl)
					.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER
							.equals(e.getUrl()))
					.collect(Collectors.toList());

			if (parentOrganizationRolePractitioners.size() == 1)
			{
				Extension parentOrganizationRolePractitioner = parentOrganizationRolePractitioners.get(0);
				List<Extension> parentOrganizations = parentOrganizationRolePractitioner.getExtension().stream()
						.filter(Extension::hasUrl)
						.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PARENT_ORGANIZATION
								.equals(e.getUrl()))
						.collect(Collectors.toList());
				List<Extension> organizationRoles = parentOrganizationRolePractitioner.getExtension().stream()
						.filter(Extension::hasUrl)
						.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_ORGANIZATION_ROLE
								.equals(e.getUrl()))
						.collect(Collectors.toList());
				List<Extension> practitionerRoles = parentOrganizationRolePractitioner.getExtension().stream()
						.filter(Extension::hasUrl)
						.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER_PRACTITIONER_ROLE
								.equals(e.getUrl()))
						.collect(Collectors.toList());

				if (parentOrganizations.size() == 1 && organizationRoles.size() == 1 && practitionerRoles.size() == 1)
				{
					Extension parentOrganization = parentOrganizations.get(0);
					Extension organizationRole = organizationRoles.get(0);
					Extension practitionerRole = practitionerRoles.get(0);

					if (parentOrganization.hasValue()
							&& parentOrganization.getValue() instanceof Identifier parentOrganizationIdentifier
							&& organizationRole.hasValue()
							&& organizationRole.getValue() instanceof Coding organizationRoleCoding
							&& practitionerRole.hasValue()
							&& practitionerRole.getValue() instanceof Coding practitionerRoleCoding
							&& OrganizationIdentifier.SID.equals(parentOrganizationIdentifier.getSystem())
							&& organizationWithIdentifierExists.test(parentOrganizationIdentifier)
							&& organizationRoleExists.test(organizationRoleCoding)
							&& practitionerRoleExists.test(practitionerRoleCoding))
					{
						return Optional.of(new Role(true, parentOrganizationIdentifier.getValue(),
								organizationRoleCoding.getSystem(), organizationRoleCoding.getCode(),
								practitionerRoleCoding.getSystem(), practitionerRoleCoding.getCode()));
					}
				}
			}
		}

		return Optional.empty();
	}
}