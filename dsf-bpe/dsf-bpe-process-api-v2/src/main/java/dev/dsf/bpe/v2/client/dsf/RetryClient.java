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
