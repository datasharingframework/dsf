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

import static dev.dsf.common.config.network.HostSpecParser.parse;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.junit.Test;

public class StaticCidrMatcherTest
{
	private InetSocketAddress ofLiteral(String literal)
	{
		return ofLiteral(literal, 0);
	}

	private InetSocketAddress ofLiteral(String literal, int port)
	{
		return new InetSocketAddress(InetAddress.ofLiteral(literal), port);
	}

	@Test
	public void shouldMatchIpv4AddressWithinCidr() throws Exception
	{
		StaticCidrMatcher matcher = StaticCidrMatcher.of(parse("10.0.0.0/24"));

		assertTrue(matcher.matches(ofLiteral("10.0.0.1")));
		assertTrue(matcher.matches(ofLiteral("10.0.0.254")));

		assertFalse(matcher.matches(ofLiteral("10.0.1.1")));
		assertFalse(matcher.matches(ofLiteral("192.168.1.1")));
	}

	@Test
	public void shouldMatchIpv4AddressWithinCidrAndPort() throws Exception
	{
		StaticCidrMatcher matcher = StaticCidrMatcher.of(parse("10.0.0.0/24:80"));

		assertTrue(matcher.matches(ofLiteral("10.0.0.1", 80)));
		assertTrue(matcher.matches(ofLiteral("10.0.0.254", 80)));

		assertFalse(matcher.matches(ofLiteral("10.0.0.1")));
		assertFalse(matcher.matches(ofLiteral("10.0.0.254")));

		assertFalse(matcher.matches(ofLiteral("10.0.0.1", 443)));
		assertFalse(matcher.matches(ofLiteral("10.0.0.254", 443)));

		assertFalse(matcher.matches(ofLiteral("10.0.1.1")));
		assertFalse(matcher.matches(ofLiteral("192.168.1.1")));
	}

	@Test
	public void shouldMatchIpv4ExactAddressUsing32() throws Exception
	{
		StaticCidrMatcher matcher = StaticCidrMatcher.of(parse("10.0.0.42/32"));

		assertTrue(matcher.matches(ofLiteral("10.0.0.42")));

		assertFalse(matcher.matches(ofLiteral("10.0.0.41")));
		assertFalse(matcher.matches(ofLiteral("10.0.0.43")));
	}

	@Test
	public void shouldMatchIpv4ExactAddress() throws Exception
	{
		StaticCidrMatcher matcher = StaticCidrMatcher.of(parse("10.0.0.42"));

		assertTrue(matcher.matches(ofLiteral("10.0.0.42")));

		assertFalse(matcher.matches(ofLiteral("10.0.0.41")));
		assertFalse(matcher.matches(ofLiteral("10.0.0.43")));
	}

	@Test
	public void shouldMatchIpv6AddressWithinCidr() throws Exception
	{
		StaticCidrMatcher matcher = StaticCidrMatcher.of(parse("[2001:db8:1234::/64]"));

		assertTrue(matcher.matches(ofLiteral("2001:db8:1234::1")));
		assertTrue(matcher.matches(ofLiteral("2001:db8:1234:0:abcd::1")));

		assertFalse(matcher.matches(ofLiteral("2001:db8:1235::1")));
	}

	@Test
	public void shouldMatchIpv6AddressWithinCidrAndPort() throws Exception
	{
		StaticCidrMatcher matcher = StaticCidrMatcher.of(parse("[2001:db8:1234::/64]:80"));

		assertTrue(matcher.matches(ofLiteral("2001:db8:1234::1", 80)));
		assertTrue(matcher.matches(ofLiteral("2001:db8:1234:0:abcd::1", 80)));

		assertFalse(matcher.matches(ofLiteral("2001:db8:1234::1")));
		assertFalse(matcher.matches(ofLiteral("2001:db8:1234:0:abcd::1")));

		assertFalse(matcher.matches(ofLiteral("2001:db8:1234::1", 443)));
		assertFalse(matcher.matches(ofLiteral("2001:db8:1234:0:abcd::1", 443)));

		assertFalse(matcher.matches(ofLiteral("2001:db8:1235::1")));
	}

	@Test
	public void shouldMatchIpv6ExactAddressUsing128() throws Exception
	{
		StaticCidrMatcher matcher = StaticCidrMatcher.of(parse("[2001:db8::42/128]"));

		assertTrue(matcher.matches(ofLiteral("2001:db8::42")));

		assertFalse(matcher.matches(ofLiteral("2001:db8::41")));
		assertFalse(matcher.matches(ofLiteral("2001:db8::43")));
	}

	@Test
	public void shouldMatchIpv6ExactAddress() throws Exception
	{
		StaticCidrMatcher matcher = StaticCidrMatcher.of(parse("[2001:db8::42]"));

		assertTrue(matcher.matches(ofLiteral("2001:db8::42")));

		assertFalse(matcher.matches(ofLiteral("2001:db8::41")));
		assertFalse(matcher.matches(ofLiteral("2001:db8::43")));
	}

	@Test
	public void shouldNotMatchIpv4NetworkAgainstIpv6Address() throws Exception
	{
		StaticCidrMatcher matcher = StaticCidrMatcher.of(parse("10.0.0.0/8"));

		assertFalse(matcher.matches(ofLiteral("2001:db8::1")));
	}

	@Test
	public void shouldNotMatchIpv6NetworkAgainstIpv4Address() throws Exception
	{
		StaticCidrMatcher matcher = StaticCidrMatcher.of(parse("[2001:db8::/32]"));

		assertFalse(matcher.matches(ofLiteral("10.0.0.1")));
	}

	@Test
	public void shouldSupportMultipleTrustedNetworks() throws Exception
	{
		InetSocketAddressMatcher matcher = StaticCidrMatcher.of(parse("10.0.0.0/24"), parse("172.16.0.0/16"),
				parse("[2001:db8::/64]"));

		assertTrue(matcher.matches(ofLiteral("10.0.0.42")));
		assertTrue(matcher.matches(ofLiteral("172.16.42.10")));
		assertTrue(matcher.matches(ofLiteral("2001:db8::1")));

		assertFalse(matcher.matches(ofLiteral("192.168.1.1")));
		assertFalse(matcher.matches(ofLiteral("2001:db9::1")));
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldRejectInvalidCidrPrefix()
	{
		StaticCidrMatcher.of(parse("10.0.0.0/33"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldRejectInvalidIpv6CidrPrefix()
	{
		StaticCidrMatcher.of(parse("2001:db8::/129"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldRejectMalformedCidr()
	{
		StaticCidrMatcher.of(parse("10.0.0.0/not-a-number"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldRejectUnknownHostname()
	{
		StaticCidrMatcher.of(parse("this-host-definitely-does-not-exist.invalid"));
	}

	@Test
	public void nullAddressDoesNotMatch()
	{
		StaticCidrMatcher matcher = StaticCidrMatcher.of(parse("10.0.0.0/24"));

		assertFalse(matcher.matches(null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldNotResolveHostnameInsideCidr()
	{
		StaticCidrMatcher.of(parse("proxy.example.com/24"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldRejectMalformedIpv4Literal()
	{
		StaticCidrMatcher.of(parse("10.0.0.999/32"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void shouldRejectMalformedIpv6Literal()
	{
		StaticCidrMatcher.of(parse("2001:db8"));
	}

	@Test
	public void matchesIpv4CidrWithPartialByte() throws Exception
	{
		StaticCidrMatcher matcher = StaticCidrMatcher.of(parse("192.168.1.128/25"));

		assertTrue(matcher.matches(ofLiteral("192.168.1.128")));
		assertTrue(matcher.matches(ofLiteral("192.168.1.200")));
		assertTrue(matcher.matches(ofLiteral("192.168.1.255")));

		assertFalse(matcher.matches(ofLiteral("192.168.1.127")));
		assertFalse(matcher.matches(ofLiteral("192.168.1.1")));
	}

	@Test
	public void matchesIpv6CidrWithPartialByte() throws Exception
	{
		StaticCidrMatcher matcher = StaticCidrMatcher.of(parse("[2001:db8::/57]"));

		// First address: 2001:0db8:0000:0000:0000:0000:0000:0000
		assertTrue(matcher.matches(ofLiteral("2001:db8::")));

		// Last address: 2001:0db8:0000:007f:ffff:ffff:ffff:ffff
		assertTrue(matcher.matches(ofLiteral("2001:db8:0:7f:ffff:ffff:ffff:ffff")));

		// First address outside: 2001:0db8:0000:0080:0000:0000:0000:0000
		assertFalse(matcher.matches(ofLiteral("2001:db8:0:80::")));
	}

	@Test
	public void shouldNotMatchUnresolvedAddress() throws Exception
	{
		InetSocketAddressMatcher matcher = StaticCidrMatcher.of(parse("10.0.0.0/24"));

		assertFalse(matcher.matches(InetSocketAddress.createUnresolved("unresolved.invalid", 666)));
	}
}
