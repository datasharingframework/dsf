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

public interface RetryClient<T>
{
	int RETRY_ONCE = 1;
	int RETRY_FOREVER = -1;
	Duration FIVE_SECONDS = Duration.ofSeconds(5);

	/**
	 * retries once after a delay of {@link RetryClient#FIVE_SECONDS}
	 *
	 * @return T
	 */
	default T withRetry()
	{
		return withRetry(RETRY_ONCE, FIVE_SECONDS);
	}

	/**
	 * retries <b>nTimes</b> and waits {@link RetryClient#FIVE_SECONDS} between tries
	 *
	 * @param nTimes
	 *            {@code >= 0}
	 * @return T
	 *
	 * @throws IllegalArgumentException
	 *             if given <b>nTimes</b> is {@code <0}
	 */
	default T withRetry(int nTimes)
	{
		return withRetry(nTimes, FIVE_SECONDS);
	}

	/**
	 * retries once after the given delay
	 *
	 * @param delay
	 *            not <code>null</code>, not {@link Duration#isNegative()}
	 * @return T
	 * @throws IllegalArgumentException
	 *             if given <b>delay</b> is <code>null</code> or {@link Duration#isNegative()}
	 */
	default T withRetry(Duration delay)
	{
		return withRetry(RETRY_ONCE, delay);
	}

	/**
	 * @param nTimes
	 *            {@code >= 0}
	 * @param delay
	 *            not <code>null</code>, not {@link Duration#isNegative()}
	 * @return T
	 *
	 * @throws IllegalArgumentException
	 *             if given <b>nTimes</b> or <b>delay</b> is <code>null</code> or {@link Duration#isNegative()}
	 */
	T withRetry(int nTimes, Duration delay);

	/**
	 * @param delay
	 *            not <code>null</code>, not {@link Duration#isNegative()}
	 * @return T
	 * @throws IllegalArgumentException
	 *             if given <b>delay</b> is <code>null</code> or {@link Duration#isNegative()}
	 */
	T withRetryForever(Duration delay);
}
