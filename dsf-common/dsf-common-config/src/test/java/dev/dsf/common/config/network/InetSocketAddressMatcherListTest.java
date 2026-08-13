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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Test;

public class InetSocketAddressMatcherListTest
{
	@Test
	public void constructorAcceptsNull() throws Exception
	{
		new InetSocketAddressMatcherList((Stream<InetSocketAddressMatcher>) null);
		new InetSocketAddressMatcherList((Collection<InetSocketAddressMatcher>) null);
	}

	@Test
	public void getNetworksReturnsUnmodifiableList() throws Exception
	{
		assertThrows(UnsupportedOperationException.class,
				() -> new InetSocketAddressMatcherList((Stream<InetSocketAddressMatcher>) null).getNetworks()
						.add(InetSocketAddressMatcher.ALL));

		assertThrows(UnsupportedOperationException.class,
				() -> new InetSocketAddressMatcherList((Collection<InetSocketAddressMatcher>) null).getNetworks()
						.add(InetSocketAddressMatcher.ALL));

		assertThrows(UnsupportedOperationException.class,
				() -> new InetSocketAddressMatcherList(Stream.of(InetSocketAddressMatcher.ALL)).getNetworks()
						.add(InetSocketAddressMatcher.ALL));

		assertThrows(UnsupportedOperationException.class,
				() -> new InetSocketAddressMatcherList(List.of(InetSocketAddressMatcher.ALL)).getNetworks()
						.add(InetSocketAddressMatcher.ALL));
	}

	@Test
	public void nullAddressDoesNotMatch() throws Exception
	{
		assertFalse(new InetSocketAddressMatcherList().matches(null));
		assertFalse(new InetSocketAddressMatcherList(InetSocketAddressMatcher.ALL).matches(null));
	}

	@Test
	public void matchesMatchesAnyConfiguredRule() throws Exception
	{
		assertTrue(new InetSocketAddressMatcherList(InetSocketAddressMatcher.NONE, InetSocketAddressMatcher.ALL)
				.matches(InetSocketAddress.createUnresolved("localhost", 0)));
		assertTrue(new InetSocketAddressMatcherList(InetSocketAddressMatcher.ALL, InetSocketAddressMatcher.NONE)
				.matches(InetSocketAddress.createUnresolved("localhost", 0)));
	}
}
