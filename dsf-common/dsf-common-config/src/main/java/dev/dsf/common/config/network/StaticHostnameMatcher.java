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

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import dev.dsf.common.config.network.HostSpecParser.HostSpec;

public final class StaticHostnameMatcher implements InetSocketAddressMatcher
{
	public static InetSocketAddressMatcherList of(HostSpec... hostSpecs)
	{
		return of(hostSpecs == null ? List.of() : Arrays.asList(hostSpecs));
	}

	public static InetSocketAddressMatcherList of(Collection<HostSpec> hostSpecs)
	{
		return new InetSocketAddressMatcherList(hostSpecs == null ? List.of()
				: hostSpecs.stream().filter(HostSpec::isDomainOrWildcard).map(StaticHostnameMatcher::of).toList());
	}

	public static StaticHostnameMatcher of(HostSpec hostSpec)
	{
		Objects.requireNonNull(hostSpec, "hostSpec");

		return new StaticHostnameMatcher(compile(hostSpec));
	}

	private final MatchPattern pattern;

	private StaticHostnameMatcher(MatchPattern pattern)
	{
		this.pattern = Objects.requireNonNull(pattern, "pattern");
	}

	private static MatchPattern compile(HostSpec hostSpec)
	{
		return switch (hostSpec.kind())
		{
			case DOMAIN -> new ExactPattern(hostSpec.host(), hostSpec.port());
			case DOMAIN_WILDCARD_SINGLE_LEVEL -> new SingleLevelWildcardPattern(hostSpec.host(), hostSpec.port());
			case DOMAIN_WILDCARD_MULTI_LEVEL -> new MultiLevelWildcardPattern(hostSpec.host(), hostSpec.port());

			default -> throw new IllegalArgumentException("hostSpec.kind '" + hostSpec.kind() + "' not supported");
		};
	}

	private static boolean portMatches(Integer expectedPort, int candidatePort)
	{
		return expectedPort == null || expectedPort == candidatePort;
	}

	private static String toOptionalPortString(String hostname, Integer port)
	{
		return hostname + (port == null ? "" : ":" + port);
	}

	private interface MatchPattern
	{
		boolean matches(String hostname, int port);
	}

	/**
	 * Example: test.invalid, matches test.invalid, does not match foo.test.invalid
	 */
	private record ExactPattern(String hostname, Integer port) implements MatchPattern
	{
		@Override
		public boolean matches(String candidateHostname, int candidatePort)
		{
			return hostname.equalsIgnoreCase(candidateHostname) && portMatches(port, candidatePort);
		}

		@Override
		public final String toString()
		{
			return toOptionalPortString(hostname, port);
		}
	}

	/**
	 * Example: *.test.invalid, matches foo.test.invalid, bar.test.invalid, does not match foo.bar.test.invalid,
	 * test.invalid
	 */
	private record SingleLevelWildcardPattern(String suffix, Integer port) implements MatchPattern
	{
		@Override
		public boolean matches(String candidateHostname, int candidatePort)
		{
			if (!portMatches(port, candidatePort))
				return false;

			String prefix = subdomainPrefix(candidateHostname);

			return prefix != null && prefix.indexOf('.') < 0;
		}

		private String subdomainPrefix(String candidateHostname)
		{
			if (candidateHostname.length() <= suffix.length() + 1)
				return null;

			int suffixStart = candidateHostname.length() - suffix.length();

			if (!candidateHostname.regionMatches(true, suffixStart, suffix, 0, suffix.length()))
				return null;

			if (candidateHostname.charAt(suffixStart - 1) != '.')
				return null;

			return candidateHostname.substring(0, suffixStart - 1);
		}

		@Override
		public final String toString()
		{
			return "*." + toOptionalPortString(suffix, port);
		}
	}

	/**
	 * Example: **.test.invalid, matches foo.test.invalid, bar.test.invalid, foo.bar.test.invalid, does not match
	 * test.invalid
	 */
	private record MultiLevelWildcardPattern(String suffix, Integer port) implements MatchPattern
	{
		@Override
		public boolean matches(String candidateHostname, int candidatePort)
		{
			if (!portMatches(port, candidatePort))
				return false;

			if (candidateHostname.length() <= suffix.length() + 1)
				return false;

			int suffixStart = candidateHostname.length() - suffix.length();

			return candidateHostname.regionMatches(true, suffixStart, suffix, 0, suffix.length())
					&& candidateHostname.charAt(suffixStart - 1) == '.';
		}

		@Override
		public final String toString()
		{
			return "**." + toOptionalPortString(suffix, port);
		}
	}

	@Override
	public boolean matches(InetSocketAddress address)
	{
		if (address == null)
			return false;

		String hostname = address.getHostString();
		int port = address.getPort();

		return pattern.matches(hostname, port);
	}

	@Override
	public String toString()
	{
		return pattern.toString();
	}
}