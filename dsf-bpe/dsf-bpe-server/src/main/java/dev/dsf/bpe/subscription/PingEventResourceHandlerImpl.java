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

import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingEventResourceHandlerImpl<R extends Resource> implements PingEventResourceHandler<R>
{
	private static final Logger logger = LoggerFactory.getLogger(PingEventResourceHandlerImpl.class);

	private final ExistingResourceLoader<R> loader;

	public PingEventResourceHandlerImpl(ExistingResourceLoader<R> loader)
	{
		this.loader = loader;
	}

	@Override
	public void onPing(String ping, String subscriptionIdPart, Map<String, List<String>> searchCriteriaQueryParameters)
	{
		logger.trace("Ping for subscription {} received", ping);
		if (!subscriptionIdPart.equals(ping))
		{
			logger.warn("Received ping for subscription {}, but expected subscription {}, ignoring ping", ping,
					subscriptionIdPart);
			return;
		}

		loader.readExistingResources(searchCriteriaQueryParameters);
	}
}
