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
package dev.dsf.common.config.network;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

public class DynamicHostnameMatcherTest
{
	private static final Duration REFRESH_TIMEOUT = Duration.ofMinutes(5);

	@Test
	public void matchesResolvedAddress() throws Exception
	{
		DynamicHostnameMatcher matcher = matcher(clockAt("2026-01-01T00:00:00Z"),
				_ -> new InetAddress[] { InetAddress.ofLiteral("192.0.2.10") });

		assertTrue(matcher.matches(new InetSocketAddress("192.0.2.10", 0)));
	}

	@Test
	public void doesNotMatchDifferentAddress() throws Exception
	{
		DynamicHostnameMatcher matcher = matcher(clockAt("2026-01-01T00:00:00Z"),
				_ -> new InetAddress[] { InetAddress.ofLiteral("192.0.2.10") });

		assertFalse(matcher.matches(new InetSocketAddress("192.0.2.11", 0)));
	}

	@Test
	public void doesNotMatchUnresolvedAddress()
	{
		DynamicHostnameMatcher matcher = matcher(clockAt("2026-01-01T00:00:00Z"),
				_ -> new InetAddress[] { InetAddress.getLoopbackAddress() });

		assertFalse(matcher.matches(InetSocketAddress.createUnresolved("example.com", 0)));
	}

	@Test
	public void refreshesAfterTimeout() throws Exception
	{
		Instant initialTime = Instant.parse("2026-01-01T00:00:00Z");
		AtomicReference<Instant> currentTime = new AtomicReference<>(initialTime);

		InetAddress firstAddress = InetAddress.ofLiteral("192.0.2.10");
		AtomicReference<InetAddress[]> resolved = new AtomicReference<>(new InetAddress[] { firstAddress });

		DynamicHostnameMatcher matcher = matcher(new MutableClock(currentTime), _ -> resolved.get());

		assertTrue(matcher.matches(new InetSocketAddress("192.0.2.10", 443)));

		// Change DNS result, but cache has not expired
		InetAddress secondAddress = InetAddress.ofLiteral("192.0.2.20");
		resolved.set(new InetAddress[] { secondAddress });
		currentTime.set(initialTime.plus(Duration.ofMinutes(4)));

		assertTrue(matcher.matches(new InetSocketAddress("192.0.2.10", 443)));

		// Cache expired, so DNS is resolved again
		currentTime.set(initialTime.plus(Duration.ofMinutes(6)));

		assertFalse(matcher.matches(new InetSocketAddress("192.0.2.10", 443)));
		assertTrue(matcher.matches(new InetSocketAddress("192.0.2.20", 443)));
	}

	@Test
	public void failsClosedWhenRefreshFails() throws Exception
	{
		Instant initialTime = Instant.parse("2026-01-01T00:00:00Z");
		AtomicReference<Instant> currentTime = new AtomicReference<>(initialTime);

		InetAddress allowed = InetAddress.ofLiteral("192.0.2.10");

		AtomicReference<Boolean> fail = new AtomicReference<>(false);
		DynamicHostnameMatcher matcher = matcher(new MutableClock(currentTime), _ ->
		{
			if (fail.get())
				throw new UnknownHostException("DNS temporarily unavailable");

			return new InetAddress[] { allowed };
		});

		// Initially the address is allowed
		assertTrue(matcher.matches(new InetSocketAddress("192.0.2.10", 443)));

		// Cause the next DNS refresh to fail
		fail.set(true);

		currentTime.set(initialTime.plus(Duration.ofMinutes(6)));

		// The old address must no longer be authorized
		assertFalse(matcher.matches(new InetSocketAddress("192.0.2.10", 443)));
	}

	@Test
	public void recoversAfterFailedRefresh() throws Exception
	{
		Instant initialTime = Instant.parse("2026-01-01T00:00:00Z");
		AtomicReference<Instant> currentTime = new AtomicReference<>(initialTime);

		InetAddress allowed = InetAddress.ofLiteral("192.0.2.10");

		AtomicReference<Boolean> fail = new AtomicReference<>(true);
		DynamicHostnameMatcher matcher = matcher(new MutableClock(currentTime), _ ->
		{
			if (fail.get())
			{
				throw new UnknownHostException("DNS failure");
			}
			return new InetAddress[] { allowed };
		});

		// Constructor refresh failed, so access is denied
		assertFalse(matcher.matches(new InetSocketAddress("192.0.2.10", 443)));

		// Allow DNS to recover
		fail.set(false);

		currentTime.set(initialTime.plus(Duration.ofMinutes(6)));

		assertTrue(matcher.matches(new InetSocketAddress("192.0.2.10", 443)));
	}

	private static DynamicHostnameMatcher matcher(Clock clock, DynamicHostnameMatcher.DnsResolver resolver)
	{
		return new DynamicHostnameMatcher(REFRESH_TIMEOUT, "example.com", clock, resolver);
	}

	private static Clock clockAt(String instant)
	{
		return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
	}

	private static final class MutableClock extends Clock
	{
		private final AtomicReference<Instant> currentTime;

		private MutableClock(AtomicReference<Instant> currentTime)
		{
			this.currentTime = currentTime;
		}

		@Override
		public ZoneOffset getZone()
		{
			return ZoneOffset.UTC;
		}

		@Override
		public Clock withZone(java.time.ZoneId zone)
		{
			return this;
		}

		@Override
		public Instant instant()
		{
			return currentTime.get();
		}
	}
}
