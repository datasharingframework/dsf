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
import static org.junit.Assert.fail;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class StaticHostnameMatcherTest
{
	@Test
	public void exactHostnameMatches()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("example.com"));

		assertMatches(matcher, "example.com", 80);
		assertMatches(matcher, "EXAMPLE.COM", 80);
	}

	@Test
	public void exactHostnameDoesNotMatchDifferentHostname()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("example.com"));

		assertNotMatches(matcher, "other.com", 80);
		assertNotMatches(matcher, "sub.example.com", 80);
	}

	@Test
	public void hostnameWithoutPortMatchesAnyPort()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("example.com"));

		assertMatches(matcher, "example.com", 0);
		assertMatches(matcher, "example.com", 80);
		assertMatches(matcher, "example.com", 443);
		assertMatches(matcher, "example.com", 65535);
	}

	@Test
	public void exactHostnameAndPortMatches()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("example.com:443"));

		assertMatches(matcher, "example.com", 443);
		assertMatches(matcher, "EXAMPLE.COM", 443);
	}

	@Test
	public void exactHostnameAndPortDoesNotMatchDifferentPort()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("example.com:443"));

		assertNotMatches(matcher, "example.com", 80);
		assertNotMatches(matcher, "example.com", 444);
	}

	@Test
	public void singleLevelWildcardMatchesOneSubdomain()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("*.example.com"));

		assertMatches(matcher, "www.example.com", 80);
		assertMatches(matcher, "api.example.com", 443);
		assertMatches(matcher, "WWW.EXAMPLE.COM", 80);
	}

	@Test
	public void singleLevelWildcardDoesNotMatchBaseHostname()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("*.example.com"));

		assertNotMatches(matcher, "example.com", 80);
	}

	@Test
	public void singleLevelWildcardDoesNotMatchNestedSubdomain()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("*.example.com"));

		assertNotMatches(matcher, "foo.www.example.com", 80);
		assertNotMatches(matcher, "a.b.example.com", 80);
	}

	@Test
	public void singleLevelWildcardDoesNotMatchSimilarSuffix()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("*.example.com"));

		assertNotMatches(matcher, "www.notexample.com", 80);
		assertNotMatches(matcher, "www.example.com.evil.com", 80);
	}

	@Test
	public void singleLevelWildcardWithPortMatchesCorrectPort()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("*.example.com:443"));

		assertMatches(matcher, "www.example.com", 443);
		assertNotMatches(matcher, "www.example.com", 80);
		assertNotMatches(matcher, "foo.www.example.com", 443);
	}

	@Test
	public void recursiveWildcardMatchesOneOrMoreSubdomains()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("**.example.com"));

		assertMatches(matcher, "www.example.com", 80);
		assertMatches(matcher, "api.example.com", 80);
		assertMatches(matcher, "foo.www.example.com", 80);
		assertMatches(matcher, "a.b.c.example.com", 80);
	}

	@Test
	public void recursiveWildcardDoesNotMatchBaseHostname()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("**.example.com"));

		assertNotMatches(matcher, "example.com", 80);
	}

	@Test
	public void recursiveWildcardDoesNotMatchSimilarSuffix()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("**.example.com"));

		assertNotMatches(matcher, "evil-example.com", 80);
		assertNotMatches(matcher, "foo.example.com.evil.com", 80);
	}

	@Test
	public void recursiveWildcardWithPortMatchesCorrectPort()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("**.example.com:443"));

		assertMatches(matcher, "www.example.com", 443);
		assertMatches(matcher, "foo.www.example.com", 443);

		assertNotMatches(matcher, "www.example.com", 80);
		assertNotMatches(matcher, "foo.www.example.com", 8443);
		assertNotMatches(matcher, "example.com", 443);
	}

	@Test
	public void leadingAndTrailingWhitespaceIsIgnored()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("  example.com:443  "));

		assertMatches(matcher, "example.com", 443);
	}

	@Test
	public void blankHostnamesAreIgnored()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse(" ", "", "\t", "example.com"));

		assertMatches(matcher, "example.com", 80);
	}

	@Test
	public void nullHostnameElementsAreIgnoredWhenUsingList()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse(Arrays.asList(null, "example.com", null)));

		assertMatches(matcher, "example.com", 80);
		assertNotMatches(matcher, "other.com", 80);
	}

	@Test
	public void nullListProducesMatcherWithNoPatterns()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse((java.util.List<String>) null));

		assertNotMatches(matcher, "example.com", 80);
	}

	@Test
	public void emptyListProducesMatcherWithNoPatterns()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse(Collections.emptyList()));

		assertNotMatches(matcher, "example.com", 80);
	}

	@Test
	public void nullAddressDoesNotMatch()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("example.com"));

		assertFalse(matcher.matches(null));
	}

	@Test
	public void portZeroIsValid()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("example.com:0"));

		assertMatches(matcher, "example.com", 0);
		assertNotMatches(matcher, "example.com", 1);
	}

	@Test
	public void maximumPortIsValid()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("example.com:65535"));

		assertMatches(matcher, "example.com", 65535);
	}

	@Test
	public void portAboveMaximumIsRejected()
	{
		assertInvalid("example.com:65536");
	}

	@Test
	public void nonNumericPortIsRejected()
	{
		assertInvalid("example.com:abc");
	}

	@Test
	public void negativePortIsRejected()
	{
		assertInvalid("example.com:-1");
	}

	@Test
	public void emptyWildcardSuffixIsRejected()
	{
		assertInvalid("*.");
		assertInvalid("**.");
	}

	@Test
	public void singleColonIsRejected()
	{
		assertInvalid(":");
	}

	@Test
	public void colonWithoutPortIsRejected()
	{
		assertInvalid("example.com:");
	}

	@Test
	public void multipleColonsAreRejected()
	{
		assertInvalid("example.com::443");
	}

	@Test
	public void multiplePatternsMatchIfAnyPatternMatches()
	{
		InetSocketAddressMatcher matcher = StaticHostnameMatcher.of(parse("localhost"), parse("*.example.com"),
				parse("server.example.org:443"));

		assertMatches(matcher, "localhost", 80);
		assertMatches(matcher, "localhost", 443);
		assertMatches(matcher, "www.example.com", 80);
		assertMatches(matcher, "www.example.com", 443);
		assertMatches(matcher, "server.example.org", 443);

		assertNotMatches(matcher, "server.example.org", 80);
		assertNotMatches(matcher, "other.org", 443);
	}

	private static void assertMatches(InetSocketAddressMatcher matcher, String hostname, int port)
	{
		assertTrue("Expected " + hostname + ":" + port + " to match",
				matcher.matches(InetSocketAddress.createUnresolved(hostname, port)));
	}

	private static void assertNotMatches(InetSocketAddressMatcher matcher, String hostname, int port)
	{
		assertFalse("Expected " + hostname + ":" + port + " not to match",
				matcher.matches(InetSocketAddress.createUnresolved(hostname, port)));
	}

	private static void assertInvalid(String pattern)
	{
		try
		{
			StaticHostnameMatcher.of(parse(pattern));
			fail("Expected IllegalArgumentException for: " + pattern);
		}
		catch (IllegalArgumentException expected)
		{
			// Expected.
		}
	}
}
