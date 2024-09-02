package dev.dsf.bpe.v2.service.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.StringType;

import dev.dsf.bpe.v2.constants.CodeSystems.ProcessAuthorization;

public class ProcessAuthorizationHelperImpl implements ProcessAuthorizationHelper
{
	private static final String EXTENSION_PROCESS_AUTHORIZATION = "http://dsf.dev/fhir/StructureDefinition/extension-process-authorization";
	private static final String EXTENSION_PROCESS_AUTHORIZATION_MESSAGE_NAME = "message-name";
	private static final String EXTENSION_PROCESS_AUTHORIZATION_TASK_PROFILE = "task-profile";
	private static final String EXTENSION_PROCESS_AUTHORIZATION_REQUESTER = "requester";
	private static final String EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT = "recipient";

	private static final class RecipientFactoryImpl implements RecipientFactory
	{
		@Override
		public Recipient localAll()
		{
			return new All(true, null, null);
		}

		@Override
		public Recipient localOrganization(String organizationIdentifier)
		{
			return new Organization(true, organizationIdentifier, null, null);
		}

		@Override
		public Recipient localRole(String parentOrganizationIdentifier, String roleSystem, String roleCode)
		{
			return new Role(true, parentOrganizationIdentifier, roleSystem, roleCode, null, null);
		}
	}

	private static final class RequesterFactoryImpl implements RequesterFactory
	{
		@Override
		public Requester localAll()
		{
			return all(true, null, null);
		}

		@Override
		public Requester localAllPractitioner(String practitionerRoleSystem, String practitionerRoleCode)
		{
			return all(true, practitionerRoleSystem, practitionerRoleCode);
		}

		@Override
		public Requester remoteAll()
		{
			return all(false, null, null);
		}

		private Requester all(boolean localIdentity, String userRoleSystem, String userRoleCode)
		{
			return new All(localIdentity, userRoleSystem, userRoleCode);
		}

		@Override
		public Requester localOrganization(String organizationIdentifier)
		{
			return organization(true, organizationIdentifier, null, null);
		}

		@Override
		public Requester localOrganizationPractitioner(String organizationIdentifier, String practitionerRoleSystem,
				String practitionerRoleCode)
		{
			return organization(true, organizationIdentifier, practitionerRoleSystem, practitionerRoleCode);
		}

		@Override
		public Requester remoteOrganization(String organizationIdentifier)
		{
			return organization(false, organizationIdentifier, null, null);
		}

		private Requester organization(boolean localIdentity, String organizationIdentifier,
				String practitionerRoleSystem, String practitionerRoleCode)
		{
			return new Organization(localIdentity, organizationIdentifier, practitionerRoleSystem,
					practitionerRoleCode);
		}

		@Override
		public Requester localRole(String parentOrganizationIdentifier, String organizatioRoleSystem,
				String organizatioRoleCode)
		{
			return role(true, parentOrganizationIdentifier, organizatioRoleSystem, organizatioRoleCode, null, null);
		}

		@Override
		public Requester localRolePractitioner(String parentOrganizationIdentifier, String organizatioRoleSystem,
				String organizatioRoleCode, String practitionerRoleSystem, String practitionerRoleCode)
		{
			return role(true, parentOrganizationIdentifier, organizatioRoleSystem, organizatioRoleCode,
					practitionerRoleSystem, practitionerRoleCode);
		}

		@Override
		public Requester remoteRole(String parentOrganizationIdentifier, String organizatioRoleSystem,
				String organizatioRoleCode)
		{
			return role(false, parentOrganizationIdentifier, organizatioRoleSystem, organizatioRoleCode, null, null);
		}

		private Requester role(boolean localIdentity, String parentOrganizationIdentifier, String organizatioRoleSystem,
				String organizatioRoleCode, String practitionerRoleSystem, String practitionerRoleCode)
		{
			return new Role(localIdentity, parentOrganizationIdentifier, organizatioRoleSystem, organizatioRoleCode,
					practitionerRoleSystem, practitionerRoleCode);
		}
	}

	private static final RecipientFactory RECIPIENT_FACTORY = new RecipientFactoryImpl();
	private static final RequesterFactory REQUESTER_FACTORY = new RequesterFactoryImpl();

	@Override
	public RecipientFactory getRecipientFactory()
	{
		return RECIPIENT_FACTORY;
	}

	@Override
	public RequesterFactory getRequesterFactory()
	{
		return REQUESTER_FACTORY;
	}

	@Override
	public ActivityDefinition add(ActivityDefinition activityDefinition, String messageName, String taskProfile,
			Requester requester, Recipient recipient)
	{
		Objects.requireNonNull(activityDefinition, "activityDefinition");
		Objects.requireNonNull(messageName, "messageName");
		if (messageName.isBlank())
			throw new IllegalArgumentException("messageName blank");
		Objects.requireNonNull(taskProfile, "taskProfile");
		if (taskProfile.isBlank())
			throw new IllegalArgumentException("taskProfile blank");
		Objects.requireNonNull(requester, "requester");
		Objects.requireNonNull(recipient, "recipient");

		Extension extension = getExtensionByMessageNameAndTaskProfile(activityDefinition, messageName, taskProfile);
		if (!hasAuthorization(extension, requester))
			extension.addExtension(requester.toRequesterExtension());
		if (!hasAuthorization(extension, recipient))
			extension.addExtension(recipient.toRecipientExtension());

		return activityDefinition;
	}

	@Override
	public ActivityDefinition add(ActivityDefinition activityDefinition, String messageName, String taskProfile,
			Collection<? extends Requester> requesters, Collection<? extends Recipient> recipients)
	{
		Objects.requireNonNull(activityDefinition, "activityDefinition");
		Objects.requireNonNull(messageName, "messageName");
		if (messageName.isBlank())
			throw new IllegalArgumentException("messageName blank");
		Objects.requireNonNull(taskProfile, "taskProfile");
		if (taskProfile.isBlank())
			throw new IllegalArgumentException("taskProfile blank");
		Objects.requireNonNull(requesters, "requesters");
		if (requesters.isEmpty())
			throw new IllegalArgumentException("requesters empty");
		Objects.requireNonNull(recipients, "recipients");
		if (recipients.isEmpty())
			throw new IllegalArgumentException("recipients empty");

		Extension extension = getExtensionByMessageNameAndTaskProfile(activityDefinition, messageName, taskProfile);
		requesters.stream().filter(r -> !hasAuthorization(extension, r))
				.forEach(r -> extension.addExtension(r.toRequesterExtension()));
		recipients.stream().filter(r -> !hasAuthorization(extension, r))
				.forEach(r -> extension.addExtension(r.toRecipientExtension()));

		return activityDefinition;
	}

	private Extension getExtensionByMessageNameAndTaskProfile(ActivityDefinition a, String messageName,
			String taskProfile)
	{
		return a.getExtension().stream().filter(Extension::hasUrl)
				.filter(e -> EXTENSION_PROCESS_AUTHORIZATION.equals(e.getUrl())).filter(Extension::hasExtension)
				.filter(e -> hasMessageName(e, messageName) && hasTaskProfileExact(e, taskProfile)).findFirst()
				.orElseGet(() ->
				{
					Extension e = newExtension(messageName, taskProfile);
					a.addExtension(e);
					return e;
				});
	}

	private boolean hasMessageName(Extension processAuthorization, String messageName)
	{
		return processAuthorization.getExtension().stream().filter(Extension::hasUrl)
				.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_MESSAGE_NAME.equals(e.getUrl()))
				.filter(Extension::hasValue).filter(e -> e.getValue() instanceof StringType)
				.map(e -> (StringType) e.getValue()).anyMatch(s -> messageName.equals(s.getValueAsString()));
	}

	private boolean hasTaskProfileExact(Extension processAuthorization, String taskProfile)
	{
		return processAuthorization.getExtension().stream().filter(Extension::hasUrl)
				.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_TASK_PROFILE.equals(e.getUrl()))
				.filter(Extension::hasValue).filter(e -> e.getValue() instanceof CanonicalType)
				.map(e -> (CanonicalType) e.getValue()).anyMatch(c -> taskProfile.equals(c.getValueAsString()));
	}

	private Extension newExtension(String messageName, String taskProfile)
	{
		Extension e = new Extension(EXTENSION_PROCESS_AUTHORIZATION);
		e.addExtension(newMessageName(messageName));
		e.addExtension(newTaskProfile(taskProfile));

		return e;
	}

	private Extension newMessageName(String messageName)
	{
		return new Extension(EXTENSION_PROCESS_AUTHORIZATION_MESSAGE_NAME).setValue(new StringType(messageName));
	}

	private Extension newTaskProfile(String taskProfile)
	{
		return new Extension(EXTENSION_PROCESS_AUTHORIZATION_TASK_PROFILE).setValue(new CanonicalType(taskProfile));
	}

	private boolean hasAuthorization(Extension processAuthorization, Requester authorization)
	{
		return processAuthorization.getExtension().stream().anyMatch(authorization::requesterMatches);
	}

	private boolean hasAuthorization(Extension processAuthorization, Recipient authorization)
	{
		return processAuthorization.getExtension().stream().anyMatch(authorization::recipientMatches);
	}

	@Override
	public boolean isValid(ActivityDefinition activityDefinition, Predicate<CanonicalType> profileExists,
			Predicate<Coding> practitionerRoleExists, Predicate<Identifier> organizationWithIdentifierExists,
			Predicate<Coding> organizationRoleExists)
	{
		if (activityDefinition == null)
			return false;

		List<Extension> processAuthorizations = activityDefinition.getExtension().stream().filter(Extension::hasUrl)
				.filter(e -> EXTENSION_PROCESS_AUTHORIZATION.equals(e.getUrl())).collect(Collectors.toList());

		if (processAuthorizations.isEmpty())
			return false;

		return processAuthorizations.stream()
				.map(e -> isProcessAuthorizationValid(e, profileExists, practitionerRoleExists,
						organizationWithIdentifierExists, organizationRoleExists))
				.allMatch(v -> v) && messageNamesUnique(processAuthorizations);
	}

	private boolean messageNamesUnique(List<Extension> processAuthorizations)
	{
		return processAuthorizations.size() == processAuthorizations.stream().flatMap(e -> e.getExtension().stream()
				.filter(mn -> EXTENSION_PROCESS_AUTHORIZATION_MESSAGE_NAME.equals(mn.getUrl())).map(Extension::getValue)
				.map(v -> (StringType) v).map(StringType::getValueAsString).findFirst().stream()).distinct().count();
	}

	private boolean isProcessAuthorizationValid(Extension processAuthorization, Predicate<CanonicalType> profileExists,
			Predicate<Coding> practitionerRoleExists, Predicate<Identifier> organizationWithIdentifierExists,
			Predicate<Coding> organizationRoleExists)
	{
		if (processAuthorization == null || !EXTENSION_PROCESS_AUTHORIZATION.equals(processAuthorization.getUrl())
				|| !processAuthorization.hasExtension())
			return false;

		List<Extension> messageNames = new ArrayList<>(), taskProfiles = new ArrayList<>(),
				requesters = new ArrayList<>(), recipients = new ArrayList<>();
		for (Extension extension : processAuthorization.getExtension())
		{
			if (extension.hasUrl())
			{
				switch (extension.getUrl())
				{
					case EXTENSION_PROCESS_AUTHORIZATION_MESSAGE_NAME:
						messageNames.add(extension);
						break;
					case EXTENSION_PROCESS_AUTHORIZATION_TASK_PROFILE:
						taskProfiles.add(extension);
						break;
					case EXTENSION_PROCESS_AUTHORIZATION_REQUESTER:
						requesters.add(extension);
						break;
					case EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT:
						recipients.add(extension);
						break;
				}
			}
		}

		if (messageNames.size() != 1 || taskProfiles.size() != 1 || requesters.isEmpty() || recipients.isEmpty())
			return false;

		return isMessageNameValid(messageNames.get(0)) && isTaskProfileValid(taskProfiles.get(0), profileExists)
				&& isRequestersValid(requesters, practitionerRoleExists, organizationWithIdentifierExists,
						organizationRoleExists)
				&& isRecipientsValid(recipients, organizationWithIdentifierExists, organizationRoleExists);
	}

	private boolean isMessageNameValid(Extension messageName)
	{
		if (messageName == null || !EXTENSION_PROCESS_AUTHORIZATION_MESSAGE_NAME.equals(messageName.getUrl()))
			return false;

		return messageName.hasValue() && messageName.getValue() instanceof StringType value
				&& !value.getValueAsString().isBlank();
	}

	private boolean isTaskProfileValid(Extension taskProfile, Predicate<CanonicalType> profileExists)
	{
		if (taskProfile == null || !EXTENSION_PROCESS_AUTHORIZATION_TASK_PROFILE.equals(taskProfile.getUrl()))
			return false;

		return taskProfile.hasValue() && taskProfile.getValue() instanceof CanonicalType value
				&& profileExists.test(value);
	}

	private boolean isRequestersValid(List<Extension> requesters, Predicate<Coding> practitionerRoleExists,
			Predicate<Identifier> organizationWithIdentifierExists, Predicate<Coding> organizationRoleExists)
	{
		return requesters.stream().allMatch(r -> isRequesterValid(r, practitionerRoleExists,
				organizationWithIdentifierExists, organizationRoleExists));
	}

	private boolean isRequesterValid(Extension requester, Predicate<Coding> practitionerRoleExists,
			Predicate<Identifier> organizationWithIdentifierExists, Predicate<Coding> organizationRoleExists)
	{
		if (requester == null || !EXTENSION_PROCESS_AUTHORIZATION_REQUESTER.equals(requester.getUrl()))
			return false;

		if (requester.hasValue() && requester.getValue() instanceof Coding value)
		{
			return requesterFrom(value, practitionerRoleExists, organizationWithIdentifierExists,
					organizationRoleExists).isPresent();
		}

		return false;
	}

	private Optional<Requester> requesterFrom(Coding coding, Predicate<Coding> practitionerRoleExists,
			Predicate<Identifier> organizationWithIdentifierExists, Predicate<Coding> organizatioRoleExists)
	{
		switch (coding.getCode())
		{
			case ProcessAuthorization.Codes.LOCAL_ALL:
			case ProcessAuthorization.Codes.LOCAL_ALL_PRACTITIONER:
			case ProcessAuthorization.Codes.REMOTE_ALL:
				return All.fromRequester(coding, practitionerRoleExists);

			case ProcessAuthorization.Codes.LOCAL_ORGANIZATION:
			case ProcessAuthorization.Codes.LOCAL_ORGANIZATION_PRACTITIONER:
			case ProcessAuthorization.Codes.REMOTE_ORGANIZATION:
				return Organization.fromRequester(coding, practitionerRoleExists, organizationWithIdentifierExists);

			case ProcessAuthorization.Codes.LOCAL_ROLE:
			case ProcessAuthorization.Codes.LOCAL_ROLE_PRACTITIONER:
			case ProcessAuthorization.Codes.REMOTE_ROLE:
				return Role.fromRequester(coding, practitionerRoleExists, organizationWithIdentifierExists,
						organizatioRoleExists);
		}

		return Optional.empty();
	}

	private boolean isRecipientsValid(List<Extension> recipients,
			Predicate<Identifier> organizationWithIdentifierExists, Predicate<Coding> organizationRoleExists)
	{
		return recipients.stream()
				.allMatch(r -> isRecipientValid(r, organizationWithIdentifierExists, organizationRoleExists));
	}

	private boolean isRecipientValid(Extension recipient, Predicate<Identifier> organizationWithIdentifierExists,
			Predicate<Coding> organizationRoleExists)
	{
		if (recipient == null || !EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT.equals(recipient.getUrl()))
			return false;

		if (recipient.hasValue() && recipient.getValue() instanceof Coding value)
		{
			return recipientFrom(value, organizationWithIdentifierExists, organizationRoleExists).isPresent();
		}

		return false;
	}

	private Optional<Recipient> recipientFrom(Coding coding, Predicate<Identifier> organizationWithIdentifierExists,
			Predicate<Coding> organizationRoleExists)
	{
		return switch (coding.getCode())
		{
			case ProcessAuthorization.Codes.LOCAL_ALL -> All.fromRecipient(coding);

			case ProcessAuthorization.Codes.LOCAL_ORGANIZATION ->
				Organization.fromRecipient(coding, organizationWithIdentifierExists);

			case ProcessAuthorization.Codes.LOCAL_ROLE ->
				Role.fromRecipient(coding, organizationWithIdentifierExists, organizationRoleExists);

			default -> Optional.empty();
		};
	}

	@Override
	public Stream<Requester> getRequesters(ActivityDefinition activityDefinition, String processUrl,
			String processVersion, String messageName, Collection<String> taskProfiles)
	{
		Optional<Extension> authorizationExtension = getAuthorizationExtension(activityDefinition, processUrl,
				processVersion, messageName, taskProfiles);

		if (authorizationExtension.isEmpty())
			return Stream.empty();
		else
			return authorizationExtension.get().getExtension().stream().filter(Extension::hasUrl)
					.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_REQUESTER.equals(e.getUrl()))
					.filter(Extension::hasValue).filter(e -> e.getValue() instanceof Coding)
					.map(e -> (Coding) e.getValue())
					.flatMap(coding -> requesterFrom(coding, c -> true, i -> true, c -> true).stream());
	}

	@Override
	public Stream<Recipient> getRecipients(ActivityDefinition activityDefinition, String processUrl,
			String processVersion, String messageName, Collection<String> taskProfiles)
	{
		Optional<Extension> authorizationExtension = getAuthorizationExtension(activityDefinition, processUrl,
				processVersion, messageName, taskProfiles);

		if (authorizationExtension.isEmpty())
			return Stream.empty();
		else
			return authorizationExtension.get().getExtension().stream().filter(Extension::hasUrl)
					.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT.equals(e.getUrl()))
					.filter(Extension::hasValue).filter(e -> e.getValue() instanceof Coding)
					.map(e -> (Coding) e.getValue())
					.flatMap(coding -> recipientFrom(coding, i -> true, c -> true).stream());
	}

	private Optional<Extension> getAuthorizationExtension(ActivityDefinition activityDefinition, String processUrl,
			String processVersion, String messageName, Collection<String> taskProfiles)
	{
		if (activityDefinition == null || processUrl == null || processUrl.isBlank() || processVersion == null
				|| processVersion.isBlank() || messageName == null || messageName.isBlank() || taskProfiles == null)
			return Optional.empty();

		if (!processUrl.equals(activityDefinition.getUrl()) || !processVersion.equals(activityDefinition.getVersion()))
			return Optional.empty();

		Optional<Extension> authorizationExtension = activityDefinition.getExtension().stream()
				.filter(Extension::hasUrl).filter(e -> EXTENSION_PROCESS_AUTHORIZATION.equals(e.getUrl()))
				.filter(Extension::hasExtension)
				.filter(e -> hasMessageName(e, messageName) && hasTaskProfile(e, taskProfiles)).findFirst();
		return authorizationExtension;
	}

	private boolean hasTaskProfile(Extension processAuthorization, Collection<String> taskProfiles)
	{
		return taskProfiles.stream()
				.anyMatch(taskProfile -> hasTaskProfileNotVersionSpecific(processAuthorization, taskProfile));
	}

	private boolean hasTaskProfileNotVersionSpecific(Extension processAuthorization, String taskProfile)
	{
		return processAuthorization.getExtension().stream().filter(Extension::hasUrl)
				.filter(e -> EXTENSION_PROCESS_AUTHORIZATION_TASK_PROFILE.equals(e.getUrl()))
				.filter(Extension::hasValue).filter(e -> e.getValue() instanceof CanonicalType)
				.map(e -> (CanonicalType) e.getValue())

				// match if task profile is equal to value in activity definition
				// or match if task profile is not version specific but value in activity definition is and non version
				// specific profiles are same -> client does not care about version of task resource, may result in
				// validation errors
				.anyMatch(c -> taskProfile.equals(c.getValueAsString())
						|| taskProfile.equals(getBase(c.getValueAsString())));
	}

	private static String getBase(String canonicalUrl)
	{
		if (canonicalUrl.contains("|"))
		{
			String[] split = canonicalUrl.split("\\|");
			return split[0];
		}
		else
			return canonicalUrl;
	}
}
