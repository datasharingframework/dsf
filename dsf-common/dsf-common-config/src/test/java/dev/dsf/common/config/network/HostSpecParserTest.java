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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import dev.dsf.common.config.network.HostSpecParser.HostSpec;
import dev.dsf.common.config.network.HostSpecParser.Kind;

@RunWith(Parameterized.class)
public class HostSpecParserTest
{
	private final String input;
	private final boolean valid;

	private final HostSpecParser.Kind expectedKind;
	private final String expectedHost;
	private final Integer expectedPrefixLength;
	private final Integer expectedPort;

	public HostSpecParserTest(String input, boolean valid, HostSpecParser.Kind expectedKind, String expectedHost,
			Integer expectedPrefixLength, Integer expectedPort)
	{
		this.input = input;
		this.valid = valid;
		this.expectedKind = expectedKind;
		this.expectedHost = expectedHost;
		this.expectedPrefixLength = expectedPrefixLength;
		this.expectedPort = expectedPort;
	}

	@Parameterized.Parameters(name = "{index}: {0}")
	public static Collection<Object[]> parameters()
	{
		return Arrays.asList(new Object[][] {

				// VALID - Domains
				valid("example.com", HostSpecParser.Kind.DOMAIN, "example.com", null, null),
				valid("example.com:80", HostSpecParser.Kind.DOMAIN, "example.com", null, 80),
				valid("EXAMPLE.COM", HostSpecParser.Kind.DOMAIN, "EXAMPLE.COM", null, null),
				valid("example.com.", HostSpecParser.Kind.DOMAIN, "example.com.", null, null),
				valid("sub.example.com:443", HostSpecParser.Kind.DOMAIN, "sub.example.com", null, 443),
				valid("a-b.example.com", HostSpecParser.Kind.DOMAIN, "a-b.example.com", null, null),
				valid("a1.example2.com", HostSpecParser.Kind.DOMAIN, "a1.example2.com", null, null),
				valid("localhost", HostSpecParser.Kind.DOMAIN, "localhost", null, null),
				valid("  example.com  ", HostSpecParser.Kind.DOMAIN, "example.com", null, null),

				// VALID - Unicode / IDN
				valid("münchen.de", HostSpecParser.Kind.DOMAIN, "münchen.de", null, null),
				valid("münchen.de:443", HostSpecParser.Kind.DOMAIN, "münchen.de", null, 443),
				valid("例え.テスト", HostSpecParser.Kind.DOMAIN, "例え.テスト", null, null),
				valid("例え.テスト:443", HostSpecParser.Kind.DOMAIN, "例え.テスト", null, 443),
				valid("παράδειγμα.δοκιμή", HostSpecParser.Kind.DOMAIN, "παράδειγμα.δοκιμή", null, null),
				valid("مثال.إختبار", HostSpecParser.Kind.DOMAIN, "مثال.إختبار", null, null),
				valid("한국.한국", HostSpecParser.Kind.DOMAIN, "한국.한국", null, null),
				valid("café.example", HostSpecParser.Kind.DOMAIN, "café.example", null, null),
				valid("école.fr", HostSpecParser.Kind.DOMAIN, "école.fr", null, null),
				valid("例123.テスト", HostSpecParser.Kind.DOMAIN, "例123.テスト", null, null),

				// VALID - One-level wildcards
				valid("*.example.com", HostSpecParser.Kind.DOMAIN_WILDCARD_SINGLE_LEVEL, "example.com", null, null),
				valid("*.example.com:443", HostSpecParser.Kind.DOMAIN_WILDCARD_SINGLE_LEVEL, "example.com", null, 443),
				valid("*.sub.example.com", HostSpecParser.Kind.DOMAIN_WILDCARD_SINGLE_LEVEL, "sub.example.com", null,
						null),
				valid("*.example.com.", HostSpecParser.Kind.DOMAIN_WILDCARD_SINGLE_LEVEL, "example.com.", null, null),
				valid("*.münchen.de", HostSpecParser.Kind.DOMAIN_WILDCARD_SINGLE_LEVEL, "münchen.de", null, null),
				valid("*.例え.テスト:443", HostSpecParser.Kind.DOMAIN_WILDCARD_SINGLE_LEVEL, "例え.テスト", null, 443),

				// VALID - Multi-level wildcards
				valid("**.example.com", HostSpecParser.Kind.DOMAIN_WILDCARD_MULTI_LEVEL, "example.com", null, null),
				valid("**.example.com:443", HostSpecParser.Kind.DOMAIN_WILDCARD_MULTI_LEVEL, "example.com", null, 443),
				valid("**.sub.example.com", HostSpecParser.Kind.DOMAIN_WILDCARD_MULTI_LEVEL, "sub.example.com", null,
						null),
				valid("**.münchen.de", HostSpecParser.Kind.DOMAIN_WILDCARD_MULTI_LEVEL, "münchen.de", null, null),
				valid("**.例え.テスト:443", HostSpecParser.Kind.DOMAIN_WILDCARD_MULTI_LEVEL, "例え.テスト", null, 443),

				// VALID - IPv4
				valid("0.0.0.0", HostSpecParser.Kind.IPV4, "0.0.0.0", 32, null),
				valid("127.0.0.1", HostSpecParser.Kind.IPV4, "127.0.0.1", 32, null),
				valid("192.168.1.1", HostSpecParser.Kind.IPV4, "192.168.1.1", 32, null),
				valid("255.255.255.255", HostSpecParser.Kind.IPV4, "255.255.255.255", 32, null),
				valid("192.168.1.1:80", HostSpecParser.Kind.IPV4, "192.168.1.1", 32, 80),
				valid("10.0.0.1:0", HostSpecParser.Kind.IPV4, "10.0.0.1", 32, 0),
				valid("10.0.0.1:65535", HostSpecParser.Kind.IPV4, "10.0.0.1", 32, 65535),

				// VALID - IPv4 CIDR
				valid("0.0.0.0/0", HostSpecParser.Kind.IPV4_CIDR, "0.0.0.0", 0, null),
				valid("192.168.1.0/24", HostSpecParser.Kind.IPV4_CIDR, "192.168.1.0", 24, null),
				valid("192.168.1.0/24:80", HostSpecParser.Kind.IPV4_CIDR, "192.168.1.0", 24, 80),
				valid("255.255.255.255/32", HostSpecParser.Kind.IPV4_CIDR, "255.255.255.255", 32, null),

				// VALID - IPv6 - Brackets are mandatory
				valid("[::1]", HostSpecParser.Kind.IPV6, "::1", 128, null),
				valid("[::]", HostSpecParser.Kind.IPV6, "::", 128, null),
				valid("[2001:db8::1]", HostSpecParser.Kind.IPV6, "2001:db8::1", 128, null),
				valid("[2001:0db8:0000:0000:0000:0000:0000:0001]", HostSpecParser.Kind.IPV6,
						"2001:0db8:0000:0000:0000:0000:0000:0001", 128, null),
				valid("[FE80::1]", HostSpecParser.Kind.IPV6, "FE80::1", 128, null),
				valid("[::1]:80", HostSpecParser.Kind.IPV6, "::1", 128, 80),
				valid("[2001:db8::1]:443", HostSpecParser.Kind.IPV6, "2001:db8::1", 128, 443),
				valid("[::1]:0", HostSpecParser.Kind.IPV6, "::1", 128, 0),
				valid("[::1]:65535", HostSpecParser.Kind.IPV6, "::1", 128, 65535),

				// VALID - IPv6 CIDR - Brackets are mandatory
				valid("[::/0]", HostSpecParser.Kind.IPV6_CIDR, "::", 0, null),
				valid("[2001:db8::/32]", HostSpecParser.Kind.IPV6_CIDR, "2001:db8::", 32, null),
				valid("[2001:db8::/32]:443", HostSpecParser.Kind.IPV6_CIDR, "2001:db8::", 32, 443),
				valid("[::1/128]", HostSpecParser.Kind.IPV6_CIDR, "::1", 128, null),
				valid("[ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff/128]:65535", HostSpecParser.Kind.IPV6_CIDR,
						"ffff:ffff:ffff:ffff:ffff:ffff:ffff:ffff", 128, 65535),

				// INVALID - Empty
				invalid(""), invalid(" "), invalid("   "), invalid("\t"), invalid("\n"),

				// INVALID - Domains
				invalid(".example.com"), invalid("example..com"), invalid("example.com.."), invalid("-example.com"),
				invalid("example-.com"), invalid("example.-com"), invalid("example.com-"), invalid("example_foo.com"),
				invalid("example!com"), invalid("example@com"), invalid("example/com"), invalid("foo..bar"),

				// INVALID - Unicode domains - Emoji is not a valid IDN letter/digit
				invalid("example.😀"),

				// INVALID - Unicode domains - Symbols are not valid domain-label characters
				invalid("例え.テスト!"), invalid("例え..テスト"),

				// INVALID - Unicode domains - Hyphen restrictions apply to Unicode labels
				invalid("-例え.テスト"), invalid("例え-.テスト"), invalid("例え.-テスト"), invalid("例え.テスト-"),

				// INVALID - Domain ports
				invalid("example.com:"), invalid("example.com:abc"), invalid("example.com:-1"),
				invalid("example.com:+1"), invalid("example.com:65536"), invalid("example.com:999999999999999999999"),
				invalid("münchen.de:"), invalid("例え.テスト:abc"), invalid("例え.テスト:65536"),

				// INVALID - Domain ports - Unicode/full-width digits are not valid port syntax
				invalid("example.com:８０"), invalid("example.com:٨٠"),

				// INVALID - Wildcards
				invalid("*"), invalid("*."), invalid("**."), invalid("*example.com"), invalid("**example.com"),
				invalid("***.example.com"), invalid("*.*.example.com"), invalid("**.*.example.com"),
				invalid("*.example.com:"), invalid("*.example.com:abc"), invalid("*.example.com:65536"),
				invalid("*.münchen.de:"), invalid("*.例え.テスト:abc"),

				// INVALID - IPv4
				invalid("1.2.3.256"), invalid("256.2.3.4"), invalid("1.256.3.4"), invalid("1.2.256.4"),
				invalid("01.2.3.4"), invalid("1.02.3.4"), invalid("1.2.03.4"), invalid("1.2.3.04"), invalid("1.2.3."),
				invalid(".1.2.3"), invalid("1..2.3"), invalid("1.2..3"),

				// VALID - Domain - that look like IPv4
				valid("1.2.3", Kind.DOMAIN, "1.2.3", null, null),
				valid("1.2.3.4.5", Kind.DOMAIN, "1.2.3.4.5", null, null),
				valid("1.2.3.a", Kind.DOMAIN, "1.2.3.a", null, null),
				valid("a.2.3.4", Kind.DOMAIN, "a.2.3.4", null, null),
				valid("1.2.3.4.", Kind.DOMAIN, "1.2.3.4.", null, null),
				valid("１２７.０.０.１", Kind.DOMAIN, "１２７.０.０.１", null, null),
				valid("127.０.０.１", Kind.DOMAIN, "127.０.０.１", null, null),

				// INVALID - IPv4 CIDR
				invalid("1.2.3.4/"), invalid("1.2.3.4/33"), invalid("1.2.3.4/-1"), invalid("1.2.3.4/+1"),
				invalid("1.2.3.4/abc"), invalid("1.2.3.4/24/25"),

				// INVALID - IPv4 CIDR - Unicode digits are not valid prefix syntax
				invalid("1.2.3.4/２４"), invalid("1.2.3.4/٢٤"),

				// INVALID - IPv4 ports
				invalid("1.2.3.4:"), invalid("1.2.3.4:abc"), invalid("1.2.3.4:-1"), invalid("1.2.3.4:+80"),
				invalid("1.2.3.4:65536"),

				// INVALID - IPv6 must be bracketed
				invalid("::1"), invalid("::"), invalid("2001:db8::1"), invalid("2001:db8::/32"), invalid("::1:80"),
				invalid("2001:db8::1:443"), invalid("2001:db8::/64:443"),

				// INVALID - Bracketed IPv6
				invalid("["), invalid("[]"), invalid("[::1"), invalid("[::1]]"), invalid("[::1]foo"),
				invalid("[::1]foo:80"), invalid("[::1]:"), invalid("[::1]:abc"), invalid("[::1]:-1"),
				invalid("[::1]:+80"), invalid("[::1]:65536"),

				// INVALID - IPv6 CIDR
				invalid("[2001:db8::/]"), invalid("[2001:db8::/129]"), invalid("[2001:db8::/-1]"),
				invalid("[2001:db8::/+1]"), invalid("[2001:db8::/abc]"), invalid("[2001:db8::/32/64]"),
				invalid("[::1/129]"),

				// Unicode digits are not valid IPv6 prefix syntax
				invalid("[::1/１２８]"), invalid("[::1/١٢٨]"),

				// INVALID - IPv6 syntax
				invalid("[1:2:3:4:5:6:7:8:9]"), invalid("[1:2:3:4:5:6:7]"), invalid("[gggg::1]"), invalid("[1:::1]"),
				invalid("[:::]"), invalid("[2001:db8:::1]"), invalid("[::1%eth0]"),

				// INVALID - Ambiguous host/port syntax
				invalid("example.com:80:90"), invalid("münchen.de:80:90"), invalid("例え.テスト:80:90"),
				invalid("1.2.3.4:80:90"), invalid("example.com::80"), invalid("1.2.3.4::80"),

				// IDNA test cases
				valid("bücher.de", HostSpecParser.Kind.DOMAIN, "bücher.de", null, null),
				valid("BÜCHER.DE", HostSpecParser.Kind.DOMAIN, "BÜCHER.DE", null, null),
				valid("e\u0301xample.com", HostSpecParser.Kind.DOMAIN, "e\u0301xample.com", null, null),
				valid("日本語.jp", HostSpecParser.Kind.DOMAIN, "日本語.jp", null, null),
				valid("مثال.إختبار", HostSpecParser.Kind.DOMAIN, "مثال.إختبار", null, null),
				valid("example。com", HostSpecParser.Kind.DOMAIN, "example。com", null, null),

				invalid("example.😀") });
	}

	private static Object[] valid(String input, HostSpecParser.Kind kind, String host, Integer prefixLength,
			Integer port)
	{
		return new Object[] { input, true, kind, host, prefixLength, port };
	}

	private static Object[] invalid(String input)
	{
		return new Object[] { input, false, null, null, null, null };
	}

	@Test
	public void parse()
	{
		if (!valid)
		{
			assertInvalid();
			return;
		}

		HostSpec actual = HostSpecParser.parse(input);

		assertEquals("kind", expectedKind, actual.kind());
		assertEquals("host", expectedHost, actual.host());
		assertEquals("prefixLength", expectedPrefixLength, actual.prefixLength());
		assertEquals("port", expectedPort, actual.port());

		assertEquals("isIp", expectedPrefixLength != null, actual.isIp());
		assertEquals("isDomain", expectedPrefixLength == null, actual.isDomainOrWildcard());

		assertEquals("hasPort", expectedPort != null, actual.hasPort());

		assertEquals("toString", input == null ? null : input.trim(), actual.toString());
	}

	private void assertInvalid()
	{
		try
		{
			HostSpecParser.parse(input);
			fail("Expected IllegalArgumentException for: " + input);
		}
		catch (IllegalArgumentException expected)
		{
			// Expected
		}
	}

	@Test
	public void parseAllPreservesOrderAndUnicode()
	{
		List<String> inputs = List.of("example.com", "münchen.de", "例え.テスト", "192.168.1.1", "[::1]:443",
				"*.παράδειγμα.δοκιμή");

		List<HostSpec> result = HostSpecParser.parse(inputs);

		assertEquals(6, result.size());
		assertEquals(HostSpecParser.Kind.DOMAIN, result.get(0).kind());
		assertEquals("münchen.de", result.get(1).host());
		assertEquals("例え.テスト", result.get(2).host());
		assertEquals(HostSpecParser.Kind.IPV4, result.get(3).kind());
		assertEquals(HostSpecParser.Kind.IPV6, result.get(4).kind());
		assertEquals(Integer.valueOf(443), result.get(4).port());
		assertEquals(HostSpecParser.Kind.DOMAIN_WILDCARD_SINGLE_LEVEL, result.get(5).kind());
		assertEquals("παράδειγμα.δοκιμή", result.get(5).host());
	}

	@Test
	public void parseAllFiltersNullElement()
	{
		List<HostSpec> list = HostSpecParser.parse(Arrays.asList("example.com", null, "192.168.1.1"));
		assertNotNull(list);
		assertEquals(2, list.size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void parseRejectsNulCharacter()
	{
		String input = "foo" + '\0' + "bar.example";
		HostSpecParser.parse(input);
	}
}