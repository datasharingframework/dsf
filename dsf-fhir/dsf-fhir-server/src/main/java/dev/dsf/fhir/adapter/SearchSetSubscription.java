package dev.dsf.fhir.adapter;

import org.hl7.fhir.r4.model.Subscription;

public class SearchSetSubscription extends AbstractSearchSet<Subscription>
{
	private record Row(ElementId id, String status, String reason, String criteria, String channelType,
			String channelPayload, String lastUpdated)
	{
	}

	public SearchSetSubscription(int defaultPageCount)
	{
		super(defaultPageCount, Subscription.class);
	}

	@Override
	protected Row toRow(ElementId id, Subscription resource)
	{
		String status = resource.hasStatus() ? resource.getStatus().toCode() : "";

		String reason = resource.hasReasonElement() && resource.getReasonElement().hasValue()
				? resource.getReasonElement().getValue()
				: "";
		String criteria = resource.hasCriteriaElement() && resource.getCriteriaElement().hasValue()
				? resource.getCriteriaElement().getValue()
				: "";
		String channelType = resource.hasChannel() && resource.getChannel().hasTypeElement()
				&& resource.getChannel().getTypeElement().hasCode() ? resource.getChannel().getTypeElement().getCode()
						: "";
		String channelPayload = resource.hasChannel() && resource.getChannel().hasPayloadElement()
				&& resource.getChannel().getPayloadElement().hasCode()
						? resource.getChannel().getPayloadElement().getCode()
						: "";

		String lastUpdated = formatLastUpdated(resource);

		return new Row(id, status, reason, criteria, channelType, channelPayload, lastUpdated);
	}
}
