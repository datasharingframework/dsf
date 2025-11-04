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

import org.hl7.fhir.r4.model.Bundle;

class PreferReturnMinimalWithRetryImpl implements PreferReturnMinimalWithRetry
{
	private final WebserviceClientJersey delegate;

	PreferReturnMinimalWithRetryImpl(WebserviceClientJersey delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public Bundle postBundle(Bundle bundle)
	{
		return delegate.postBundle(PreferReturnType.MINIMAL, bundle);
	}

	@Override
	public PreferReturnMinimal withRetry(int nTimes, Duration delay)
	{
		if (nTimes < 0)
			throw new IllegalArgumentException("nTimes < 0");
		if (delay == null || delay.isNegative())
			throw new IllegalArgumentException("delay null or negative");

		return new PreferReturnMinimalRetryImpl(delegate, nTimes, delay);
	}

	@Override
	public PreferReturnMinimal withRetryForever(Duration delay)
	{
		if (delay == null || delay.isNegative())
			throw new IllegalArgumentException("delay null or negative");

		return new PreferReturnMinimalRetryImpl(delegate, RETRY_FOREVER, delay);
	}
}