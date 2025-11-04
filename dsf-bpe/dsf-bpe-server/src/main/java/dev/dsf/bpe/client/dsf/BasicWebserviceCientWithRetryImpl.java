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
package dev.dsf.bpe.client.dsf;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

class BasicWebserviceCientWithRetryImpl extends AbstractWebserviceClientJerseyWithRetry implements BasicWebserviceClient
{
	BasicWebserviceCientWithRetryImpl(WebserviceClientJersey delegate, int nTimes, Duration delayMillis)
	{
		super(delegate, nTimes, delayMillis);
	}

	@Override
	public <R extends Resource> R update(R resource)
	{
		return retry(() -> delegate.update(resource));
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return retry(() -> delegate.postBundle(bundle));
	}

	@Override
	public Bundle searchWithStrictHandling(Class<? extends Resource> resourceType, Map<String, List<String>> parameters)
	{
		return retry(() -> delegate.searchWithStrictHandling(resourceType, parameters));
	}
}