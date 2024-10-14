package dev.dsf.fhir.authorization.process;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;

public interface ProcessAuthorizationHelper
{
	String PROCESS_AUTHORIZATION_SYSTEM = "http://dsf.dev/fhir/CodeSystem/process-authorization";
	String PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION = "LOCAL_ORGANIZATION";
	String PROCESS_AUTHORIZATION_VALUE_LOCAL_ORGANIZATION_PRACTITIONER = "LOCAL_ORGANIZATION_PRACTITIONER";
	String PROCESS_AUTHORIZATION_VALUE_REMOTE_ORGANIZATION = "REMOTE_ORGANIZATION";
	String PROCESS_AUTHORIZATION_VALUE_LOCAL_ROLE = "LOCAL_ROLE";
	String PROCESS_AUTHORIZATION_VALUE_LOCAL_ROLE_PRACTITIONER = "LOCAL_ROLE_PRACTITIONER";
	String PROCESS_AUTHORIZATION_VALUE_REMOTE_ROLE = "REMOTE_ROLE";
	String PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL = "LOCAL_ALL";
	String PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL_PRACTITIONER = "LOCAL_ALL_PRACTITIONER";
	String PROCESS_AUTHORIZATION_VALUE_REMOTE_ALL = "REMOTE_ALL";

	String ORGANIZATION_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/organization-identifier";

	String EXTENSION_PROCESS_AUTHORIZATION = "http://dsf.dev/fhir/StructureDefinition/extension-process-authorization";
	String EXTENSION_PROCESS_AUTHORIZATION_MESSAGE_NAME = "message-name";
	String EXTENSION_PROCESS_AUTHORIZATION_TASK_PROFILE = "task-profile";
	String EXTENSION_PROCESS_AUTHORIZATION_REQUESTER = "requester";
	String EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT = "recipient";

	String EXTENSION_PROCESS_AUTHORIZATION_PRACTITIONER = "http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-practitioner";

	String EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION = "http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-organization";

	String EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER = "http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-organization-practitioner";
	String EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_ORGANIZATION = "organization";
	String EXTENSION_PROCESS_AUTHORIZATION_ORGANIZATION_PRACTITIONER_PRACTITIONER_ROLE = "practitioner-role";

	String EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE = "http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-parent-organization-role";
	String EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PARENT_ORGANIZATION = "parent-organization";
	String EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_ORGANIZATION_ROLE = "organization-role";

	String EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER = "http://dsf.dev/fhir/StructureDefinition/extension-process-authorization-parent-organization-role-practitioner";
	String EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER_PARENT_ORGANIZATION = EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PARENT_ORGANIZATION;
	String EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER_ORGANIZATION_ROLE = EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_ORGANIZATION_ROLE;
	String EXTENSION_PROCESS_AUTHORIZATION_PARENT_ORGANIZATION_ROLE_PRACTITIONER_PRACTITIONER_ROLE = "practitioner-role";


	ActivityDefinition add(ActivityDefinition activityDefinition, String messageName, String taskProfile,
			Requester requester, Recipient recipient);

	ActivityDefinition add(ActivityDefinition activityDefinition, String messageName, String taskProfile,
			Collection<? extends Requester> requesters, Collection<? extends Recipient> recipients);

	boolean isValid(ActivityDefinition activityDefinition, Predicate<CanonicalType> profileExists,
			Predicate<Coding> practitionerRoleExists, Predicate<Identifier> organizationWithIdentifierExists,
			Predicate<Coding> organizationRoleExists);

	default Stream<Requester> getRequesters(ActivityDefinition activityDefinition, String processUrl,
			String processVersion, String messageName, String taskProfile)
	{
		return getRequesters(activityDefinition, processUrl, processVersion, messageName,
				Collections.singleton(taskProfile));
	}

	Stream<Requester> getRequesters(ActivityDefinition activityDefinition, String processUrl, String processVersion,
			String messageName, Collection<String> taskProfiles);

	default Stream<Recipient> getRecipients(ActivityDefinition activityDefinition, String processUrl,
			String processVersion, String messageName, String taskProfiles)
	{
		return getRecipients(activityDefinition, processUrl, processVersion, messageName,
				Collections.singleton(taskProfiles));
	}

	Stream<Recipient> getRecipients(ActivityDefinition activityDefinition, String processUrl, String processVersion,
			String messageName, Collection<String> taskProfiles);
}
