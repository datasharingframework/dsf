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
package dev.dsf.bpe.v2.client.dsf;

import java.time.Duration;

public interface DelayStrategy
{
	/**
	 * Waits for 100ms, 200ms, 400ms, 800ms, 800ms, ...
	 */
	DelayStrategy EXPONENTIAL_BACKOFF = new DelayStrategy()
	{
		@Override
		public Duration getFirstDelay()
		{
			return Duration.ofMillis(100);
		}

		@Override
		public Duration getNextDelay(Duration lastDelay)
		{
			if (Duration.ofMillis(800).compareTo(lastDelay) <= 0)
				return lastDelay;

			return lastDelay.multipliedBy(2);
		}
	};

	/**
	 * Waits for 200ms, 200ms, ...
	 */
	DelayStrategy CONSTANT = constant(Duration.ofMillis(200));

	/**
	 * @param delay
	 *            not <code>null</code>, not {@link Duration#isNegative()}
	 * @return constant strategy with the given interval between tries
	 * @throws IllegalArgumentException
	 *             if given <b>delay</b> is <code>null</code> or {@link Duration#isNegative()}
	 */
	static DelayStrategy constant(Duration delay)
	{
		if (delay == null || delay.isNegative())
			throw new IllegalArgumentException("delay null or negative");

		return new DelayStrategy()
		{
			@Override
			public Duration getFirstDelay()
			{
				return delay;
			}

			@Override
			public Duration getNextDelay(Duration lastInterval)
			{
				return delay;
			}
		};
	}

	Duration getFirstDelay();

	Duration getNextDelay(Duration lastDelay);
}