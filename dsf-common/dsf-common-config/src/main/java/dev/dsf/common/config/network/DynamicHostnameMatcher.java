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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.config.network.HostSpecParser.HostSpec;

/**
 * Matches resolved socket addresses against the IP addresses currently associated with a configured DNS domain. The
 * domain is resolved on construction and periodically refreshed after the configured timeout; unresolved socket
 * addresses are rejected and no reverse DNS lookup is performed.
 *
 * <p>
 * The refresh timeout should be chosen according to the acceptable stale-authorization window and e.g. the expected
 * reverse-proxy replacement time.
 * </p>
 * <p>
 * <b>Warning:</b> This matcher does not consider the configured {@link HostSpec#port()}
 * </p>
 */
public class DynamicHostnameMatcher implements InetSocketAddressMatcher
{
	private static final Logger logger = LoggerFactory.getLogger(DynamicHostnameMatcher.DnsResolver.class);

	@FunctionalInterface
	public interface DnsResolver
	{
		InetAddress[] resolve(String domain) throws UnknownHostException;
	}

	public static InetSocketAddressMatcherList of(Duration refreshTimeout, HostSpec... hostSpecs)
	{
		return of(refreshTimeout, hostSpecs == null ? List.of() : Arrays.asList(hostSpecs));
	}

	public static InetSocketAddressMatcherList of(Duration refreshTimeout, Collection<HostSpec> hostSpecs)
	{
		return new InetSocketAddressMatcherList(hostSpecs == null ? List.of()
				: hostSpecs.stream().filter(HostSpec::isDomain).map(host -> of(refreshTimeout, host)).toList());
	}

	public static DynamicHostnameMatcher of(Duration refreshTimeout, HostSpec hostSpec)
	{
		Objects.requireNonNull(refreshTimeout, "refreshTimeout");
		Objects.requireNonNull(hostSpec, "hostSpec");

		return new DynamicHostnameMatcher(refreshTimeout, hostSpec.host());
	}

	private final ReentrantLock refreshLock = new ReentrantLock();
	private final Duration refreshTimeout;

	private final String hostname;

	private final Clock clock;
	private final DnsResolver dnsResolver;

	private volatile Set<InetAddress> addresses = Set.of();
	private volatile Instant expiresAt = Instant.MIN;

	private DynamicHostnameMatcher(Duration refreshTimeout, String hostname)
	{
		this(refreshTimeout, hostname, Clock.systemUTC(), InetAddress::getAllByName);
	}

	// package private for testing
	DynamicHostnameMatcher(Duration refreshTimeout, String hostname, Clock clock, DnsResolver dnsResolver)
	{
		Objects.requireNonNull(refreshTimeout, "refreshTimeout");
		Objects.requireNonNull(hostname, "hostname");

		if (refreshTimeout == null || refreshTimeout.isZero() || refreshTimeout.isNegative())
			throw new IllegalArgumentException("refreshTimeout must be greater than zero");
		if (hostname.isBlank())
			throw new IllegalArgumentException("hostname must not be blank");

		this.refreshTimeout = refreshTimeout;

		this.hostname = hostname;

		this.clock = clock;
		this.dnsResolver = dnsResolver;

		refresh();
	}

	@Override
	public boolean matches(InetSocketAddress address)
	{
		if (address == null || address.isUnresolved())
			return false;

		refreshIfExpired();

		return addresses.contains(address.getAddress());
	}

	private void refreshIfExpired()
	{
		if (clock.instant().isBefore(expiresAt))
			return;

		refresh();
	}

	private void refresh()
	{
		refreshLock.lock();
		try
		{
			if (clock.instant().isBefore(expiresAt))
				return;

			try
			{
				addresses = Set.copyOf(List.of(dnsResolver.resolve(hostname)));
			}
			catch (UnknownHostException | RuntimeException e)
			{
				logger.warn("Unable to resolve domain '{}': {} - {}", hostname, e.getClass().getName(), e.getMessage());

				// DNS failure means no address is authorized
				addresses = Set.of();
			}
			expiresAt = clock.instant().plus(refreshTimeout);
		}
		finally
		{
			refreshLock.unlock();
		}
	}

	@Override
	public String toString()
	{
		return hostname;
	}
}