/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
