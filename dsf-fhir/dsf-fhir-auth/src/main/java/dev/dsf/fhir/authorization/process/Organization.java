package dev.dsf.fhir.authorization.process;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Reference;

import dev.dsf.common.auth.Identity;
import dev.dsf.fhir.authorization.read.ReadAccessHelper;

public class Organization implements Recipient, Requester
{
	private final String organizationIdentifier;
	private final boolean localIdentity;

	public Organization(boolean localIdentity, String organizationIdentifier)
	{
		Objects.requireNonNull(organizationIdentifier, "organizationIdentifier");
		if (organizationIdentifier.isBlank())
			throw new IllegalArgumentException("organizationIdentifier blank");

		this.localIdentity = localIdentity;
		this.organizationIdentifier = organizationIdentifier;
	}

	@Override
	public boolean isRequesterAuthorized(Identity requesterUser, Stream<OrganizationAffiliation> requesterAffiliations)
	{
		return isAuthorized(requesterUser);
	}

	@Override
	public boolean isRecipientAuthorized(Identity recipientUser, Stream<OrganizationAffiliation> recipientAffiliations)
	{
		return isAuthorized(recipientUser);
	}

	private boolean isAuthorized(Identity identity)
	{
		return identity != null && identity.getOrganization() != null && identity.getOrganization().getActive()
				&& identity.isLocalIdentity() == localIdentity && hasOrganizationIdentifier(identity.getOrganization());
	}

	private boolean hasOrganizationIdentifier(org.hl7.fhir.r4.model.Organization organization)
	{
		return organization.getIdentifier().stream().filter(Identifier::hasSystem).filter(Identifier::hasValue)
				.filter(i -> ReadAccessHelper.ORGANIZATION_IDENTIFIER_SYSTEM.equals(i.getSystem()))
				.anyMatch(i -> organizationIdentifier.equals(i.getValue()));
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
		Identifier organization = new Reference().getIdentifier()
				.setSystem(ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM).setValue(organizationIdentifier);

		Coding coding = getProcessAuthorizationCode();
		coding.addExtension().setUrl(ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION)
				.setValue(organization);
		return coding;
	}

	@Override
	public boolean requesterMatches(Extension requesterExtension)
	{
		return matches(requesterExtension, ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_REQUESTER);
	}

	@Override
	public boolean recipientMatches(Extension recipientExtension)
	{
		return matches(recipientExtension, ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT);
	}

	private boolean matches(Extension recipientExtension, String url)
	{
		return recipientExtension != null && url.equals(recipientExtension.getUrl()) && recipientExtension.hasValue()
				&& recipientExtension.getValue() instanceof Coding && matches((Coding) recipientExtension.getValue())
				&& recipientExtension.getValue().hasExtension()
				&& hasMatchingOrganizationExtension(recipientExtension.getValue().getExtension());
	}

	private boolean hasMatchingOrganizationExtension(List<Extension> extensions)
	{
		return extensions.stream().anyMatch(this::organizationExtensionMatches);
	}

	private boolean organizationExtensionMatches(Extension extension)
	{
		return ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION.equals(extension.getUrl())
				&& extension.hasValue() && extension.getValue() instanceof Identifier
				&& organizationIdentifierMatches((Identifier) extension.getValue());
	}

	private boolean organizationIdentifierMatches(Identifier identifier)
	{
		return identifier != null && identifier.hasSystem() && identifier.hasValue()
				&& ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM.equals(identifier.getSystem())
				&& organizationIdentifier.equals(identifier.getValue());
	}

	@Override
	public Coding getProcessAuthorizationCode()
	{
		if (localIdentity)
			return new Coding(ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM,
					ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION, null);
		else
			return new Coding(ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM,
					ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_REMOTE_ORGANIZATION, null);
	}

	@Override
	public boolean matches(Coding processAuthorizationCode)
	{
		if (localIdentity)
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

	@SuppressWarnings("unchecked")
	public static Optional<Requester> fromRequester(Coding coding,
			Predicate<Identifier> organizationWithIdentifierExists)
	{
		return (Optional<Requester>) from(coding, organizationWithIdentifierExists);
	}

	@SuppressWarnings("unchecked")
	public static Optional<Recipient> fromRecipient(Coding coding,
			Predicate<Identifier> organizationWithIdentifierExists)
	{
		return (Optional<Recipient>) from(coding, organizationWithIdentifierExists);
	}

	private static Optional<? super Organization> from(Coding coding,
			Predicate<Identifier> organizationWithIdentifierExists)
	{
		if (coding != null && coding.hasSystem()
				&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM.equals(coding.getSystem())
				&& coding.hasCode())
		{
			if (ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION.equals(coding.getCode()))
				return from(true, coding, organizationWithIdentifierExists);
			else if (ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_REMOTE_ORGANIZATION
					.equals(coding.getCode()))
				return from(false, coding, organizationWithIdentifierExists);
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
				if (organization.hasValue() && organization.getValue() instanceof Identifier)
				{
					Identifier identifier = (Identifier) organization.getValue();
					if (ProcessAuthorizationHelper.ORGANIZATION_IDENTIFIER_SYSTEM.equals(identifier.getSystem())
							&& organizationWithIdentifierExists.test(identifier))
						return Optional.of(new Organization(localIdentity, identifier.getValue()));
				}
			}
		}

		return Optional.empty();
	}
}