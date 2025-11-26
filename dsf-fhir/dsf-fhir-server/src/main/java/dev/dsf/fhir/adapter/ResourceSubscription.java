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
