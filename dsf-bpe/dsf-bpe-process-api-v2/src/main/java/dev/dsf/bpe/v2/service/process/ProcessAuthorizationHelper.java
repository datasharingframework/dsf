package dev.dsf.bpe.v2.service.process;

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
	interface RecipientFactory
	{
		Recipient localAll();

		Recipient localOrganization(String organizationIdentifier);

		Recipient localRole(String parentOrganizationIdentifier, String roleSystem, String roleCode);
	}

	interface RequesterFactory
	{
		Requester localAll();

		Requester localAllPractitioner(String practitionerRoleSystem, String practitionerRoleCode);

		Requester remoteAll();

		Requester localOrganization(String organizationIdentifier);

		Requester localOrganizationPractitioner(String organizationIdentifier, String practitionerRoleSystem,
				String practitionerRoleCode);

		Requester remoteOrganization(String organizationIdentifier);

		Requester localRole(String parentOrganizationIdentifier, String organizatioRoleSystem,
				String organizatioRoleCode);

		Requester localRolePractitioner(String parentOrganizationIdentifier, String organizatioRoleSystem,
				String organizatioRoleCode, String practitionerRoleSystem, String practitionerRoleCode);

		Requester remoteRole(String parentOrganizationIdentifier, String organizatioRoleSystem,
				String organizatioRoleCode);
	}

	RecipientFactory getRecipientFactory();

	RequesterFactory getRequesterFactory();

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
