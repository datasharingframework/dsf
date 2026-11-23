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
package dev.dsf.bpe.subscription;

import java.util.Objects;

import org.hl7.fhir.r4.model.Subscription;

import ca.uhn.fhir.rest.api.Constants;

public enum EventType
{
	XML, JSON, PING;

	public static EventType fromSubscription(Subscription subscription)
	{
		Objects.requireNonNull(subscription, "subscription");
		if (!subscription.hasChannel())
			throw new IllegalArgumentException("subscription has no channel");

		String payload = subscription.getChannel().getPayload();

		if (payload == null)
			return EventType.PING;

		return switch (payload)
		{
			case Constants.CT_FHIR_JSON, Constants.CT_FHIR_JSON_NEW -> EventType.JSON;
			case Constants.CT_FHIR_XML, Constants.CT_FHIR_XML_NEW -> EventType.XML;

			default -> throw new RuntimeException("Unsupported subscription.payload " + payload);
		};
	}
}
