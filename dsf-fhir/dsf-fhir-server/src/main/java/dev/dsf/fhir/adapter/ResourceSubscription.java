package dev.dsf.fhir.adapter;

import org.hl7.fhir.r4.model.Subscription;

public class ResourceSubscription extends AbstractResource<Subscription>
{
	private record Element(String reason, String criteria, String channelType, String channelPayload)
	{
	}

	public ResourceSubscription()
	{
		super(Subscription.class,
				ActiveOrStatus.status(Subscription::hasStatusElement, Subscription::getStatusElement));
	}

	@Override
	protected Element toElement(Subscription resource)
	{
		String reason = getString(resource, Subscription::hasReasonElement, Subscription::getReasonElement);
		String criteria = getString(resource, Subscription::hasCriteriaElement, Subscription::getCriteriaElement);

		String channelType = resource.hasChannel() && resource.getChannel().hasTypeElement()
				&& resource.getChannel().getTypeElement().hasCode() ? resource.getChannel().getTypeElement().getCode()
						: "";

		String channelPayload = resource.hasChannel() && resource.getChannel().hasPayloadElement()
				&& resource.getChannel().getPayloadElement().hasCode()
						? resource.getChannel().getPayloadElement().getCode()
						: "";

		return new Element(reason, criteria, channelType, channelPayload);
	}
}
