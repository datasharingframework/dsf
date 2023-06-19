package dev.dsf.fhir.authorization.process;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Reference;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;

public class Role implements Recipient, Requester
{
	private final boolean localIdentity;
	private final String parentOrganizationIdentifier;
	private final String roleSystem;
	private final String roleCode;

	public Role(boolean localIdentity, String parentOrganizationIdentifier, String roleSystem, String roleCode)
	{
		Objects.requireNonNull(parentOrganizationIdentifier, "parentOrganizationIdentifier");
		if (parentOrganizationIdentifier.isBlank())
			throw new IllegalArgumentException("parentOrganizationIdentifier blank");
		Objects.requireNonNull(roleSystem, "roleSystem");
		if (roleSystem.isBlank())
			throw new IllegalArgumentException("roleSystem blank");
		Objects.requireNonNull(roleCode, "roleCode");
		if (roleCode.isBlank())
			throw new IllegalArgumentException("roleCode blank");

		this.localIdentity = localIdentity;
		this.parentOrganizationIdentifier = parentOrganizationIdentifier;
		this.roleSystem = roleSystem;
		this.roleCode = roleCode;
	}

	@Override
	public boolean isRequesterAuthorized(Identity requesterUser, Stream<OrganizationAffiliation> requesterAffiliations)
	{
		return isAuthorized(requesterUser, requesterAffiliations);
	}

	@Override
	public boolean isRecipientAuthorized(Identity recipientUser, Stream<OrganizationAffiliation> recipientAffiliations)
	{
		return isAuthorized(recipientUser, recipientAffiliations);
	}

	private boolean isAuthorized(Identity identity, Stream<OrganizationAffiliation> affiliations)
	{
		return identity != null && identity.getOrganization() != null && identity.getOrganization().getActive()
				&& identity.isLocalIdentity() == localIdentity && affiliations != null
				&& hasParentOrganizationMemberRole(identity.getOrganization(), affiliations);
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
				.filter(a -> ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM
						.equals(a.getOrganization().getIdentifier().getSystem()))
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
				.filter(Coding::hasCode)
				.anyMatch(c -> c.getSystem().equals(roleSystem) && c.getCode().equals(roleCode));
	}

	@Override
	public Extension toRecipientExtension()
	{
		return new Extension().setUrl(ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT)
				.setValue(toCoding());
	}

	@Override
	public Extension toRequesterExtension()
	{
		return new Extension().setUrl(ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_REQUESTER)
				.setValue(toCoding());
	}

	private Coding toCoding()
	{
		Identifier parentOrganization = new Reference().getIdentifier()
				.setSystem(ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM)
				.setValue(parentOrganizationIdentifier);
		Extension parentOrganizationExt = new Extension(
				ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PARENT_ORGANIZATION)
				.setValue(parentOrganization);

		Coding role = new Coding(roleSystem, roleCode, null);
		Extension roleExt = new Extension(
				ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_ROLE)
				.setValue(role);

		Coding coding = getProcessAuthorizationCode();
		coding.addExtension()
				.setUrl(ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE)
				.addExtension(parentOrganizationExt).addExtension(roleExt);
		return coding;
	}

	@Override
	public boolean requesterMatches(Extension requesterExtension)
	{
		return requesterExtension != null
				&& ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_REQUESTER
						.equals(requesterExtension.getUrl())
				&& requesterExtension.hasValue() && requesterExtension.getValue() instanceof Coding
				&& matches((Coding) requesterExtension.getValue()) && requesterExtension.getValue().hasExtension()
				&& hasMatchingParentOrganizationRoleExtension(requesterExtension.getValue().getExtension());
	}

	@Override
	public boolean recipientMatches(Extension recipientExtension)
	{
		return recipientExtension != null
				&& ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT
						.equals(recipientExtension.getUrl())
				&& recipientExtension.hasValue() && recipientExtension.getValue() instanceof Coding
				&& matches((Coding) recipientExtension.getValue()) && recipientExtension.getValue().hasExtension()
				&& hasMatchingParentOrganizationRoleExtension(recipientExtension.getValue().getExtension());
	}

	private boolean hasMatchingParentOrganizationRoleExtension(List<Extension> extension)
	{
		return extension.stream().anyMatch(this::parentOrganizationRoleExtensionMatches);
	}

	private boolean parentOrganizationRoleExtensionMatches(Extension extension)
	{
		return ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE
				.equals(extension.getUrl()) && extension.hasExtension()
				&& hasMatchingParentOrganizationExtension(extension.getExtension())
				&& hasMatchingRoleExtension(extension.getExtension());
	}

	private boolean hasMatchingParentOrganizationExtension(List<Extension> extension)
	{
		return extension.stream().anyMatch(this::parentOrganizationExtensionMatches);
	}

	private boolean parentOrganizationExtensionMatches(Extension extension)
	{
		return ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PARENT_ORGANIZATION
				.equals(extension.getUrl()) && extension.hasValue() && extension.getValue() instanceof Identifier
				&& parentOrganizationIdentifierMatches((Identifier) extension.getValue());
	}

	private boolean parentOrganizationIdentifierMatches(Identifier identifier)
	{
		return identifier != null && identifier.hasSystem() && identifier.hasValue()
				&& ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM.equals(identifier.getSystem())
				&& parentOrganizationIdentifier.equals(identifier.getValue());
	}

	private boolean hasMatchingRoleExtension(List<Extension> extension)
	{
		return extension.stream().anyMatch(this::roleExtensionMatches);
	}

	private boolean roleExtensionMatches(Extension extension)
	{
		return ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_ROLE
				.equals(extension.getUrl()) && extension.hasValue() && extension.getValue() instanceof Coding
				&& roleMatches((Coding) extension.getValue());
	}

	private boolean roleMatches(Coding coding)
	{
		return coding != null && coding.hasSystem() && coding.hasCode() && roleSystem.equals(coding.getSystem())
				&& roleCode.equals(coding.getCode());
	}

	@Override
	public Coding getProcessAuthorizationCode()
	{
		if (localIdentity)
			return new Coding(ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM,
					ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ROLE, null);
		else
			return new Coding(ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM,
					ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_REMOTE_ROLE, null);
	}

	@Override
	public boolean matches(Coding processAuthorizationCode)
	{
		if (localIdentity)
			return processAuthorizationCode != null
					&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM
							.equals(processAuthorizationCode.getSystem())
					&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ROLE
							.equals(processAuthorizationCode.getCode());
		else
			return processAuthorizationCode != null
					&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM
							.equals(processAuthorizationCode.getSystem())
					&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_REMOTE_ROLE
							.equals(processAuthorizationCode.getCode());
	}

	@SuppressWarnings("unchecked")
	public static Optional<Requester> fromRequester(Coding coding,
			Predicate<Identifier> organizationWithIdentifierExists, Predicate<Coding> roleExists)
	{
		return (Optional<Requester>) from(coding, organizationWithIdentifierExists, roleExists);
	}

	@SuppressWarnings("unchecked")
	public static Optional<Recipient> fromRecipient(Coding coding,
			Predicate<Identifier> organizationWithIdentifierExists, Predicate<Coding> roleExists)
	{
		return (Optional<Recipient>) from(coding, organizationWithIdentifierExists, roleExists);
	}

	private static Optional<? super Role> from(Coding coding, Predicate<Identifier> organizationWithIdentifierExists,
			Predicate<Coding> roleExists)
	{
		if (coding != null && coding.hasSystem()
				&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM.equals(coding.getSystem())
				&& coding.hasCode())
		{
			if (ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ROLE.equals(coding.getCode()))
				return from(true, coding, organizationWithIdentifierExists, roleExists);
			else if (ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_REMOTE_ROLE.equals(coding.getCode()))
				return from(false, coding, organizationWithIdentifierExists, roleExists);
		}

		return Optional.empty();
	}

	private static Optional<? super Role> from(boolean localIdentity, Coding coding,
			Predicate<Identifier> organizationWithIdentifierExists, Predicate<Coding> roleExists)
	{
		if (coding != null && coding.hasExtension())
		{
			List<Extension> parentOrganizationRoles = coding.getExtension().stream().filter(Extension::hasUrl)
					.filter(e -> ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE
							.equals(e.getUrl()))
					.collect(Collectors.toList());
			if (parentOrganizationRoles.size() == 1)
			{
				Extension parentOrganizationRole = parentOrganizationRoles.get(0);
				List<Extension> parentOrganizations = parentOrganizationRole.getExtension().stream()
						.filter(Extension::hasUrl)
						.filter(e -> ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PARENT_ORGANIZATION
								.equals(e.getUrl()))
						.collect(Collectors.toList());
				List<Extension> roles = parentOrganizationRole.getExtension().stream().filter(Extension::hasUrl).filter(
						e -> ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_ROLE
								.equals(e.getUrl()))
						.collect(Collectors.toList());
				if (parentOrganizations.size() == 1 && roles.size() == 1)
				{
					Extension parentOrganization = parentOrganizations.get(0);
					Extension role = roles.get(0);

					if (parentOrganization.hasValue() && parentOrganization.getValue() instanceof Identifier
							&& role.hasValue() && role.getValue() instanceof Coding)
					{
						Identifier parentOrganizationIdentifier = (Identifier) parentOrganization.getValue();
						Coding roleCoding = (Coding) role.getValue();

						if (ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM
								.equals(parentOrganizationIdentifier.getSystem())
								&& organizationWithIdentifierExists.test(parentOrganizationIdentifier)
								&& roleExists.test(roleCoding))
						{
							return Optional.of(new Role(localIdentity, parentOrganizationIdentifier.getValue(),
									roleCoding.getSystem(), roleCoding.getCode()));
						}
					}
				}
			}
		}

		return Optional.empty();
	}
}