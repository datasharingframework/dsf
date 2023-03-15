package dev.dsf.fhir.authorization.process;

import java.util.Optional;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.OrganizationAffiliation;

import dev.dsf.common.auth.conf.Identity;

public class All implements Recipient, Requester
{
	private final boolean localIdentity;

	public All(boolean localIdentity)
	{
		this.localIdentity = localIdentity;
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
				&& identity.isLocalIdentity() == localIdentity;
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
				&& recipientExtension.getValue() instanceof Coding && matches((Coding) recipientExtension.getValue());
	}

	@Override
	public Coding getProcessAuthorizationCode()
	{
		if (localIdentity)
			return new Coding(ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM,
					ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL, null);
		else
			return new Coding(ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM,
					ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_REMOTE_ALL, null);
	}

	@Override
	public boolean matches(Coding processAuthorizationCode)
	{
		if (localIdentity)
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

	public static Optional<Requester> fromRequester(Coding coding)
	{
		if (coding != null && coding.hasSystem()
				&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM.equals(coding.getSystem())
				&& coding.hasCode())
		{
			if (ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL.equals(coding.getCode()))
				return Optional.of(new All(true));
			else if (ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_REMOTE_ALL.equals(coding.getCode()))
				return Optional.of(new All(false));
		}

		return Optional.empty();
	}

	public static Optional<Recipient> fromRecipient(Coding coding)
	{
		if (coding != null && coding.hasSystem()
				&& ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_SYSTEM.equals(coding.getSystem())
				&& coding.hasCode())
		{
			if (ProcessAuthorizationHelper.PROCESS_AUTHORIZATION_VALUE_LOCAL_ALL.equals(coding.getCode()))
				return Optional.of(new All(true));
			// remote not allowed for recipient
		}

		return Optional.empty();
	}
}