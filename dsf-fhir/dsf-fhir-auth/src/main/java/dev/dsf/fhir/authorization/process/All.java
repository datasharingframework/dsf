package dev.dsf.fhir.authorization.process;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.OrganizationIdentity;
import dev.dsf.common.auth.conf.PractitionerIdentity;

public class All implements Recipient, Requester
{
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
		Coding coding = getProcessAuthorizationCode();

		if (needsPractitionerRole)
			coding.addExtension().setUrl(ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PRACTITIONER)
					.setValue(new Coding(practitionerRoleSystem, practitionerRoleCode, null));

		return coding;
	}

	@Override
	public Coding getProcessAuthorizationCode()
	{
		if (localIdentity)
		{
			if (needsPractitionerRole())
				return new Coding(ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM,
						ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL_PRACTITIONER, null);
			else
				return new Coding(ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM,
						ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL, null);
		}
		else
		{
			return new Coding(ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM,
					ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_REMOTE_ALL, null);
		}
	}

	@Override
	public boolean requesterMatches(Extension requesterExtension)
	{
		return matches(requesterExtension, ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_REQUESTER)
				&& hasMatchingPractitionerExtension(requesterExtension.getValue().getExtension());
	}

	@Override
	public boolean recipientMatches(Extension recipientExtension)
	{
		return matches(recipientExtension, ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_RECIPIENT);
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
		return ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PRACTITIONER.equals(extension.getUrl())
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
				return processAuthorizationCode != null
						&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM
								.equals(processAuthorizationCode.getSystem())
						&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL_PRACTITIONER
								.equals(processAuthorizationCode.getCode());
			else
				return processAuthorizationCode != null
						&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM
								.equals(processAuthorizationCode.getSystem())
						&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL
								.equals(processAuthorizationCode.getCode());
		else
			return processAuthorizationCode != null
					&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM
							.equals(processAuthorizationCode.getSystem())
					&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_REMOTE_ALL
							.equals(processAuthorizationCode.getCode());
	}

	public static Optional<Requester> fromRequester(Coding coding, Predicate<Coding> practitionerRoleExists)
	{
		if (coding != null && coding.hasSystem()
				&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM.equals(coding.getSystem())
				&& coding.hasCode())
		{
			if (ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL.equals(coding.getCode()))
				return Optional.of(new All(true, null, null));
			else if (ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_REMOTE_ALL.equals(coding.getCode()))
				return Optional.of(new All(false, null, null));
			else if (ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL_PRACTITIONER
					.equals(coding.getCode()))
				return fromPractitionerRequester(coding, practitionerRoleExists);
		}

		return Optional.empty();
	}

	private static Optional<Requester> fromPractitionerRequester(Coding coding,
			Predicate<Coding> practitionerRoleExists)
	{
		if (coding != null && coding.hasExtension())
		{
			List<Extension> practitionerRoles = coding.getExtension().stream().filter(Extension::hasUrl).filter(
					e -> ProcessAuthorizationHelper.EXTENSION_PROCESS_AUTHORIZATION_PRACTITIONER.equals(e.getUrl()))
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
		if (coding != null && coding.hasSystem()
				&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM.equals(coding.getSystem())
				&& coding.hasCode()
				&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL.equals(coding.getCode()))
		{
			return Optional.of(new All(true, null, null));
			// remote not allowed for recipient
		}

		return Optional.empty();
	}
}