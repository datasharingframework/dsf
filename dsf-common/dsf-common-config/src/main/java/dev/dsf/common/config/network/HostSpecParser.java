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

import java.net.IDN;
import java.net.Inet6Address;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 *
 * Parses host specifications into immutable {@link HostSpec} instances.
 *
 * <p>
 * A host specification may represent a domain name, a wildcard domain, an IPv4 address, an IPv4 CIDR network, an IPv6
 * address, or an IPv6 CIDR network. All host types may specify an optional port, except that IPv6 addresses must always
 * be enclosed in square brackets when a port is present (and are required to use brackets in all cases).
 *
 * <p>
 * Examples of supported specifications:
 *
 * <table>
 * <caption>Supported host specification formats</caption>
 * <tr>
 * <th>Kind</th>
 * <th>Example</th>
 * <th>Example with port</th>
 * </tr>
 * <tr>
 * <td>{@link Kind#DOMAIN DOMAIN}</td>
 * <td>{@code example.com}</td>
 * <td>{@code example.com:443}</td>
 * </tr>
 * <tr>
 * <td>{@link Kind#DOMAIN_WILDCARD_SINGLE_LEVEL WILDCARD_DOMAIN_ONE_LEVEL}</td>
 * <td>{@code *.example.com}</td>
 * <td>{@code *.example.com:443}</td>
 * </tr>
 * <tr>
 * <td>{@link Kind#DOMAIN_WILDCARD_MULTI_LEVEL WILDCARD_DOMAIN_MULTI_LEVEL}</td>
 * <td>{@code **.example.com}</td>
 * <td>{@code **.example.com:443}</td>
 * </tr>
 * <tr>
 * <td>{@link Kind#IPV4 IPV4}</td>
 * <td>{@code 192.0.2.1}</td>
 * <td>{@code 192.0.2.1:443}</td>
 * </tr>
 * <tr>
 * <td>{@link Kind#IPV4_CIDR IPV4_CIDR}</td>
 * <td>{@code 192.0.2.0/24}</td>
 * <td>{@code 192.0.2.0/24:443}</td>
 * </tr>
 * <tr>
 * <td>{@link Kind#IPV6 IPV6}</td>
 * <td>{@code [2001:db8::1]}</td>
 * <td>{@code [2001:db8::1]:443}</td>
 * </tr>
 * <tr>
 * <td>{@link Kind#IPV6_CIDR IPV6_CIDR}</td>
 * <td>{@code [2001:db8::/32]}</td>
 * <td>{@code [2001:db8::/32]:443}</td>
 * </tr>
 * </table>
 *
 * <p>
 * Ports are optional and must be decimal values in the range {@code 0..65535}.
 *
 * <p>
 * IPv6 specifications are always enclosed in square brackets, including IPv6 specifications without a port. This
 * removes ambiguity between the IPv6 address's colons and the optional port separator.
 *
 * <p>
 * Domain names support Unicode/IDN labels according to the behavior of {@link java.net.IDN}. The original Unicode
 * representation supplied by the caller is retained in {@link HostSpec#host()}; IDN conversion is used for validation
 * and does not replace the original host value.
 *
 * <p>
 * For example:
 *
 * <pre>{@code
 *
 * HostSpecParser.parse("example.com:443");
 * HostSpecParser.parse("münchen.example:443");
 * HostSpecParser.parse("*.example.com");
 * HostSpecParser.parse("**.example.com:8443");
 * HostSpecParser.parse("192.0.2.1");
 * HostSpecParser.parse("192.0.2.0/24:8080");
 * HostSpecParser.parse("[2001:db8::1]");
 * HostSpecParser.parse("[2001:db8::1]:443");
 * HostSpecParser.parse("[2001:db8::/32]:8443");
 * }</pre>
 *
 * <p>
 * The parser trims leading and trailing whitespace from the supplied specification. A {@link NullPointerException} is
 * thrown for a {@code null} input, and an {@link IllegalArgumentException} is thrown for an empty or syntactically
 * invalid specification.
 *
 * @see HostSpec
 * @see Kind
 * @see java.net.IDN
 */
public final class HostSpecParser
{
	private static final int MAX_PORT = 65_535;
	private static final int MAX_IPV4_PREFIX = 32;
	private static final int MAX_IPV6_PREFIX = 128;

	private HostSpecParser()
	{
	}

	/**
	 * @param inputs
	 *            may be <code>null</code>, <code>null</code> or blank elements are filtered
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static List<HostSpec> parse(String... inputs)
	{
		return inputs == null ? List.of() : parse(Arrays.asList(inputs));
	}

	/**
	 * @param inputs
	 *            may be {@link NullPointerException}, <code>null</code> or blank elements are filtered
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static List<HostSpec> parse(Collection<String> inputs)
	{
		return inputs == null ? List.of()
				: inputs.stream().filter(Objects::nonNull).filter(Predicate.not(String::isBlank))
						.map(HostSpecParser::parse).toList();
	}

	/**
	 * @param input
	 *            not <code>null</code>, not blank
	 * @return
	 * @throws IllegalArgumentException
	 */
	public static HostSpec parse(String input)
	{
		Objects.requireNonNull(input, "input");
		if (input.isBlank())
			throw new IllegalArgumentException("input is blank");

		String value = input.trim();

		if (value.isEmpty())
			throw invalid(input, "Host specification is empty");

		if (value.startsWith("["))
			return parseIpv6(value);

		if (value.startsWith("**."))
			return parseWildcard(value, true);

		if (value.startsWith("*."))
			return parseWildcard(value, false);

		if (looksLikeIpv4(value))
			return parseIpv4(value);

		return parseDomain(value);
	}

	private static HostSpec parseIpv6(String input)
	{
		int closingBracket = input.indexOf(']');

		if (closingBracket < 0)
			throw invalid(input, "Missing closing ']' for IPv6 address");

		String inside = input.substring(1, closingBracket);
		String after = input.substring(closingBracket + 1);

		if (inside.isEmpty())
			throw invalid(input, "IPv6 address is empty");

		Integer port = null;

		if (!after.isEmpty())
		{
			if (!after.startsWith(":"))
				throw invalid(input, "Unexpected characters after IPv6 address");

			port = parsePort(after.substring(1), input);
		}

		int slash = inside.indexOf('/');

		if (slash >= 0)
		{
			if (inside.indexOf('/', slash + 1) >= 0)
				throw invalid(input, "Invalid IPv6 CIDR specification");

			String address = inside.substring(0, slash);
			String prefixText = inside.substring(slash + 1);

			if (address.isEmpty())
				throw invalid(input, "IPv6 address is empty");

			int prefix = parseInteger(prefixText, input, "IPv6 prefix length");

			if (prefix > MAX_IPV6_PREFIX)
				throw invalid(input, "IPv6 prefix length must be 0..128");

			if (!isValidIpv6(address))
				throw invalid(input, "Invalid IPv6 address");

			return new HostSpec(Kind.IPV6_CIDR, address, prefix, port);
		}

		if (!isValidIpv6(inside))
			throw invalid(input, "Invalid IPv6 address");

		return new HostSpec(Kind.IPV6, inside, MAX_IPV6_PREFIX, port);
	}

	private static HostSpec parseWildcard(String input, boolean multiLevel)
	{
		String prefix = multiLevel ? "**." : "*.";
		String remainder = input.substring(prefix.length());

		HostAndPort hostAndPort = splitHostAndPort(remainder);

		if (!isValidDomain(hostAndPort.host()))
			throw invalid(input, "Invalid wildcard domain");

		Kind kind = multiLevel ? Kind.DOMAIN_WILDCARD_MULTI_LEVEL : Kind.DOMAIN_WILDCARD_SINGLE_LEVEL;

		return new HostSpec(kind, hostAndPort.host(), null, hostAndPort.port());
	}

	private static boolean looksLikeIpv4(String input)
	{
		String host = input;

		int colon = host.lastIndexOf(':');

		if (colon >= 0)
			host = host.substring(0, colon);

		int slash = host.indexOf('/');

		if (slash >= 0)
			host = host.substring(0, slash);

		return isAsciiIpv4Candidate(host);
	}

	private static boolean isAsciiIpv4Candidate(String value)
	{
		if (value.isEmpty())
			return false;

		int dots = 0;

		for (int i = 0; i < value.length(); i++)
		{
			char c = value.charAt(i);

			if (c == '.')
				dots++;
			else if (c < '0' || c > '9')
				return false;
		}

		return dots == 3;
	}

	private static HostSpec parseIpv4(String input)
	{
		HostAndPort hostAndPort = splitHostAndPort(input);
		String host = hostAndPort.host();

		int slash = host.indexOf('/');

		if (slash >= 0)
		{
			if (host.indexOf('/', slash + 1) >= 0)
				throw invalid(input, "Invalid IPv4 CIDR specification");

			String address = host.substring(0, slash);
			String prefixText = host.substring(slash + 1);

			int prefix = parseInteger(prefixText, input, "IPv4 prefix length");

			if (prefix > MAX_IPV4_PREFIX)
				throw invalid(input, "IPv4 prefix length must be 0..32");

			if (!isValidIpv4(address))
				throw invalid(input, "Invalid IPv4 address");

			return new HostSpec(Kind.IPV4_CIDR, address, prefix, hostAndPort.port());
		}

		if (!isValidIpv4(host))
			throw invalid(input, "Invalid IPv4 address");

		return new HostSpec(Kind.IPV4, host, MAX_IPV4_PREFIX, hostAndPort.port());
	}

	private static HostSpec parseDomain(String input)
	{
		HostAndPort hostAndPort = splitHostAndPort(input);

		if (!isValidDomain(hostAndPort.host()))
			throw invalid(input, "Invalid domain");

		return new HostSpec(Kind.DOMAIN, hostAndPort.host(), null, hostAndPort.port());
	}

	/**
	 * Splits a hostname from its optional ":port".
	 *
	 * IPv6 is handled separately because it must be bracketed.
	 */
	private static HostAndPort splitHostAndPort(String input)
	{
		int colon = input.lastIndexOf(':');

		if (colon < 0)
			return new HostAndPort(input, null);

		String host = input.substring(0, colon);
		String portText = input.substring(colon + 1);

		if (host.isEmpty())
			throw invalid(input, "Missing host");

		/*
		 * An unbracketed host containing another colon cannot be a domain or IPv4 address. IPv6 must have been handled
		 * by parseIpv6() already.
		 */
		if (host.indexOf(':') >= 0)
			throw invalid(input, "IPv6 addresses must be bracketed");

		return new HostAndPort(host, parsePort(portText, input));
	}

	private static int parsePort(String text, String input)
	{
		int port = parseInteger(text, input, "port");

		if (port < 0 || port > MAX_PORT)
			throw invalid(input, "Port must be 0..65535");

		return port;
	}

	/**
	 * Parses an ASCII decimal integer.
	 *
	 * Unicode decimal digits are intentionally rejected for ports and CIDR prefix lengths.
	 */
	private static int parseInteger(String text, String input, String name)
	{
		if (text.isEmpty())
			throw invalid(input, "Invalid " + name);

		for (int i = 0; i < text.length(); i++)
		{
			char c = text.charAt(i);

			if (c < '0' || c > '9')
				throw invalid(input, "Invalid " + name);
		}

		try
		{
			return Integer.parseInt(text);
		}
		catch (NumberFormatException e)
		{
			throw invalid(input, "Invalid " + name);
		}
	}

	private static boolean isValidIpv4(String value)
	{
		if (!isAsciiIpv4Candidate(value))
			return false;

		String[] parts = value.split("\\.", -1);

		if (parts.length != 4)
			return false;

		for (String part : parts)
		{
			if (part.isEmpty())
				return false;

			if (part.length() > 1 && part.charAt(0) == '0')
				return false;

			int number = 0;

			for (int i = 0; i < part.length(); i++)
			{
				number = number * 10 + (part.charAt(i) - '0');

				if (number > 255)
					return false;
			}
		}

		return true;
	}

	private static boolean isValidIpv6(String value)
	{
		if (value.isEmpty())
			return false;

		/*
		 * Explicitly reject IPv6 scope identifiers. HostSpec currently represents an IP address, not an
		 * interface-scoped address.
		 */
		if (value.indexOf('%') >= 0)
			return false;

		try
		{
			return Inet6Address.ofLiteral(value) instanceof Inet6Address;
		}
		catch (IllegalArgumentException e)
		{
			return false;
		}
	}

	/**
	 * Validates the domain using Java's IDN implementation while preserving the original Unicode representation.
	 *
	 * The returned ASCII/Punycode value is deliberately discarded.
	 *
	 * USE_STD3_ASCII_RULES additionally applies the RFC 1122/1123 restrictions to ASCII labels.
	 */
	private static boolean isValidDomain(String value)
	{
		if (value.isEmpty())
			return false;

		/*
		 * A trailing root dot is valid. Java's IDN implementation handles the Unicode DNS separator characters
		 * according to its own IDN processing rules.
		 */
		try
		{
			String ascii = IDN.toASCII(value, IDN.USE_STD3_ASCII_RULES);

			if (ascii.isEmpty())
				return false;

			/*
			 * IDN.toASCII() performs IDNA processing, but keep these explicit DNS length checks here so the parser's
			 * accepted representation has a clear DNS-size boundary.
			 */
			if (ascii.length() > 255)
				return false;

			String withoutRootDot = ascii.endsWith(".") ? ascii.substring(0, ascii.length() - 1) : ascii;

			if (withoutRootDot.isEmpty())
				return false;

			String[] labels = withoutRootDot.split("\\.", -1);

			for (String label : labels)
			{
				if (label.isEmpty() || label.length() > 63)
					return false;
			}

			return true;
		}
		catch (IllegalArgumentException e)
		{
			return false;
		}
	}

	private static IllegalArgumentException invalid(String input, String reason)
	{
		return new IllegalArgumentException(reason + ": \"" + input + "\"");
	}

	public static record HostSpec(Kind kind, String host, Integer prefixLength, Integer port)
	{
		public HostSpec
		{
			Objects.requireNonNull(kind, "kind");
			Objects.requireNonNull(host, "host");

			if (kind == Kind.IPV4 && (prefixLength == null || prefixLength != MAX_IPV4_PREFIX))
				throw new IllegalArgumentException("Invalid prefix length: " + prefixLength);
			else if (kind == Kind.IPV6 && (prefixLength == null || prefixLength != MAX_IPV6_PREFIX))
				throw new IllegalArgumentException("Invalid prefix length: " + prefixLength);
			else if (prefixLength != null)
			{
				int max = switch (kind)
				{
					case IPV4, IPV4_CIDR -> MAX_IPV4_PREFIX;
					case IPV6, IPV6_CIDR -> MAX_IPV6_PREFIX;
					default -> throw new IllegalArgumentException("Prefix length is only valid for IP kinds");
				};

				if (prefixLength < 0 || prefixLength > max)
					throw new IllegalArgumentException("Invalid prefix length: " + prefixLength);
			}

			if (port != null && (port < 0 || port > MAX_PORT))
				throw new IllegalArgumentException("Invalid port: " + port);
		}

		public boolean hasPort()
		{
			return port != null;
		}

		public boolean isIp()
		{
			return kind == Kind.IPV4 || kind == Kind.IPV6 || kind == Kind.IPV4_CIDR || kind == Kind.IPV6_CIDR;
		}

		public boolean isDomain()
		{
			return kind == Kind.DOMAIN;
		}

		public boolean isDomainOrWildcard()
		{
			return isDomain() || kind == Kind.DOMAIN_WILDCARD_SINGLE_LEVEL || kind == Kind.DOMAIN_WILDCARD_MULTI_LEVEL;
		}

		public String asciiHost()
		{
			return isDomainOrWildcard() ? IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES) : host;
		}

		@Override
		public final String toString()
		{
			return switch (kind)
			{
				case Kind.DOMAIN, Kind.IPV4 -> host + (hasPort() ? ":" + port : "");
				case Kind.IPV6 -> "[" + host + "]" + (hasPort() ? ":" + port : "");

				case Kind.DOMAIN_WILDCARD_SINGLE_LEVEL -> "*." + host + (hasPort() ? ":" + port : "");
				case Kind.DOMAIN_WILDCARD_MULTI_LEVEL -> "**." + host + (hasPort() ? ":" + port : "");

				case Kind.IPV4_CIDR -> host + "/" + prefixLength + (hasPort() ? ":" + port : "");
				case Kind.IPV6_CIDR -> "[" + host + "/" + prefixLength + "]" + (hasPort() ? ":" + port : "");

				default -> throw new IllegalArgumentException("Kind not supported: " + kind);
			};
		}
	}

	public static enum Kind
	{
		DOMAIN,

		DOMAIN_WILDCARD_SINGLE_LEVEL, DOMAIN_WILDCARD_MULTI_LEVEL,

		IPV4, IPV4_CIDR,

		IPV6, IPV6_CIDR
	}

	private static record HostAndPort(String host, Integer port)
	{
	}
}