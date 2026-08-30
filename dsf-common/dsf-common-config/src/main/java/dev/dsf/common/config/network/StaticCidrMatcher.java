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

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import dev.dsf.common.config.network.HostSpecParser.HostSpec;

public class StaticCidrMatcher implements InetSocketAddressMatcher
{
	public static InetSocketAddressMatcherList of(HostSpec... hostSpecs)
	{
		return of(hostSpecs == null ? List.of() : List.of(hostSpecs));
	}

	public static InetSocketAddressMatcherList of(Collection<HostSpec> hostSpecs)
	{
		return new InetSocketAddressMatcherList(hostSpecs == null ? List.of()
				: hostSpecs.stream().filter(HostSpec::isIp).map(StaticCidrMatcher::of).toList());
	}

	public static StaticCidrMatcher of(HostSpec hostSpec)
	{
		Objects.requireNonNull(hostSpec, "hostSpec");

		return switch (hostSpec.kind())
		{
			case IPV4 -> {
				byte[] address = Inet4Address.ofLiteral(hostSpec.host()).getAddress();
				yield new StaticCidrMatcher(address, address.length * 8, hostSpec.port());
			}
			case IPV6 -> {
				byte[] address = Inet6Address.ofLiteral(hostSpec.host()).getAddress();
				yield new StaticCidrMatcher(address, address.length * 8, hostSpec.port());
			}
			case IPV4_CIDR, IPV6_CIDR -> new StaticCidrMatcher(InetAddress.ofLiteral(hostSpec.host()).getAddress(),
					hostSpec.prefixLength(), hostSpec.port());

			default -> throw new IllegalArgumentException("hostSpec.kind '" + hostSpec.kind() + "' not supported");
		};
	}

	private final byte[] network;
	private final int prefixLength;
	private final Integer port;

	private StaticCidrMatcher(byte[] network, int prefixLength, Integer port)
	{
		this.network = network;
		this.prefixLength = prefixLength;
		this.port = port;
	}

	@Override
	public boolean matches(InetSocketAddress address)
	{
		if (address == null)
			return false;

		if (address.isUnresolved())
			return false;

		if (!portMatches(port, address.getPort()))
			return false;

		byte[] candidate = address.getAddress().getAddress();

		// IPv4 and IPv6 addresses have different lengths and can never match each other
		if (candidate.length != network.length)
			return false;

		int fullBytes = prefixLength / 8;
		int remainingBits = prefixLength % 8;

		for (int i = 0; i < fullBytes; i++)
			if (candidate[i] != network[i])
				return false;

		if (remainingBits == 0)
			return true;

		int mask = 0xFF << (8 - remainingBits);

		return (candidate[fullBytes] & mask) == (network[fullBytes] & mask);
	}

	private boolean portMatches(Integer expectedPort, int candidatePort)
	{
		return expectedPort == null || expectedPort == candidatePort;
	}

	@Override
	public String toString()
	{
		try
		{
			return InetAddress.getByAddress(network).getHostAddress() + "/" + prefixLength
					+ (port == null ? "" : ":" + port);
		}
		catch (UnknownHostException e)
		{
			throw new IllegalStateException("Invalid CIDR network address", e);
		}
	}
}