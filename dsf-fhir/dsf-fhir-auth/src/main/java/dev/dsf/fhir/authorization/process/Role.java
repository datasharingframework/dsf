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

import dev.dsf.common.auth.Identity;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;

public class Role implements Recipient, Requester
{
	private final boolean localIdentity;
	private final String consortiumIdentifier;
	private final String roleSystem;
	private final String roleCode;

	public Role(boolean localIdentity, String consortiumIdentifier, String roleSystem, String roleCode)
	{
		Objects.requireNonNull(consortiumIdentifier, "consortiumIdentifier");
		if (consortiumIdentifier.isBlank())
			throw new IllegalArgumentException("consortiumIdentifier blank");
		Objects.requireNonNull(roleSystem, "roleSystem");
		if (roleSystem.isBlank())
			throw new IllegalArgumentException("roleSystem blank");
		Objects.requireNonNull(roleCode, "roleCode");
		if (roleCode.isBlank())
			throw new IllegalArgumentException("roleCode blank");

		this.localIdentity = localIdentity;
		this.consortiumIdentifier = consortiumIdentifier;
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
				&& hasConsortiumMemberRole(identity.getOrganization(), affiliations);
	}

	private boolean hasConsortiumMemberRole(org.hl7.fhir.r4.model.Organization recipientOrganization,
			Stream<OrganizationAffiliation> affiliations)
	{
		return affiliations

				// check affiliation active
				.filter(OrganizationAffiliation::getActive)

				// check consortium identifier
				.filter(OrganizationAffiliation::hasOrganization).filter(a -> a.getOrganization().hasIdentifier())
				.filter(a -> a.getOrganization().getIdentifier().hasSystem())
				.filter(a -> a.getOrganization().getIdentifier().hasValue())
				.filter(a -> ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM
						.equals(a.getOrganization().getIdentifier().getSystem()))
				.filter(a -> consortiumIdentifier.equals(a.getOrganization().getIdentifier().getValue()))

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
		Identifier consortium = new Reference().getIdentifier()
				.setSystem(ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM).setValue(consortiumIdentifier);
		Extension consortiumExt = new Extension(
				ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_CONSORTIUM_ROLE_CONSORTIUM)
				.setValue(consortium);

		Coding role = new Coding(roleSystem, roleCode, null);
		Extension roleExt = new Extension(
				ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_CONSORTIUM_ROLE_ROLE).setValue(role);

		Coding coding = getProcessAuthorizationCode();
		coding.addExtension().setUrl(ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_CONSORTIUM_ROLE)
				.addExtension(consortiumExt).addExtension(roleExt);
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
				&& hasMatchingConsortiumRoleExtension(requesterExtension.getValue().getExtension());
	}

	@Override
	public boolean recipientMatches(Extension recipientExtension)
	{
		return recipientExtension != null
				&& ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT
						.equals(recipientExtension.getUrl())
				&& recipientExtension.hasValue() && recipientExtension.getValue() instanceof Coding
				&& matches((Coding) recipientExtension.getValue()) && recipientExtension.getValue().hasExtension()
				&& hasMatchingConsortiumRoleExtension(recipientExtension.getValue().getExtension());
	}

	private boolean hasMatchingConsortiumRoleExtension(List<Extension> extension)
	{
		return extension.stream().anyMatch(this::consortiumRoleExtensionMatches);
	}

	private boolean consortiumRoleExtensionMatches(Extension extension)
	{
		return ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_CONSORTIUM_ROLE.equals(extension.getUrl())
				&& extension.hasExtension() && hasMatchingConsortiumExtension(extension.getExtension())
				&& hasMatchingRoleExtension(extension.getExtension());
	}

	private boolean hasMatchingConsortiumExtension(List<Extension> extension)
	{
		return extension.stream().anyMatch(this::consortiumExtensionMatches);
	}

	private boolean consortiumExtensionMatches(Extension extension)
	{
		return ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_CONSORTIUM_ROLE_CONSORTIUM
				.equals(extension.getUrl()) && extension.hasValue() && extension.getValue() instanceof Identifier
				&& consortiumIdentifierMatches((Identifier) extension.getValue());
	}

	private boolean consortiumIdentifierMatches(Identifier identifier)
	{
		return identifier != null && identifier.hasSystem() && identifier.hasValue()
				&& ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM.equals(identifier.getSystem())
				&& consortiumIdentifier.equals(identifier.getValue());
	}

	private boolean hasMatchingRoleExtension(List<Extension> extension)
	{
		return extension.stream().anyMatch(this::roleExtensionMatches);
	}

	private boolean roleExtensionMatches(Extension extension)
	{
		return ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_CONSORTIUM_ROLE_ROLE
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
			List<Extension> consortiumRoles = coding.getExtension().stream().filter(Extension::hasUrl).filter(
					e -> ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_CONSORTIUM_ROLE.equals(e.getUrl()))
					.collect(Collectors.toList());
			if (consortiumRoles.size() == 1)
			{
				Extension consortiumRole = consortiumRoles.get(0);
				List<Extension> consortiums = consortiumRole.getExtension().stream().filter(Extension::hasUrl).filter(
						e -> ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_CONSORTIUM_ROLE_CONSORTIUM
								.equals(e.getUrl()))
						.collect(Collectors.toList());
				List<Extension> roles = consortiumRole.getExtension().stream().filter(Extension::hasUrl)
						.filter(e -> ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_CONSORTIUM_ROLE_ROLE
								.equals(e.getUrl()))
						.collect(Collectors.toList());
				if (consortiums.size() == 1 && roles.size() == 1)
				{
					Extension consortium = consortiums.get(0);
					Extension role = roles.get(0);

					if (consortium.hasValue() && consortium.getValue() instanceof Identifier && role.hasValue()
							&& role.getValue() instanceof Coding)
					{
						Identifier consortiumIdentifier = (Identifier) consortium.getValue();
						Coding roleCoding = (Coding) role.getValue();

						if (ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM
								.equals(consortiumIdentifier.getSystem())
								&& organizationWithIdentifierExists.test(consortiumIdentifier)
								&& roleExists.test(roleCoding))
						{
							return Optional.of(new Role(localIdentity, consortiumIdentifier.getValue(),
									roleCoding.getSystem(), roleCoding.getCode()));
						}
					}
				}
			}
		}

		return Optional.empty();
	}
}