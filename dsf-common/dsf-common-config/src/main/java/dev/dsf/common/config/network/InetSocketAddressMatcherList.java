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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Matches addresses against network definitions.
 */
public class InetSocketAddressMatcherList implements InetSocketAddressMatcher
{
	private final List<InetSocketAddressMatcher> networks;

	/**
	 * @param networks
	 *            <code>null</code> values are ignored
	 */
	public InetSocketAddressMatcherList(InetSocketAddressMatcher... networks)
	{
		this(Stream.of(networks));
	}

	/**
	 * @param networks
	 *            may be <code>null</code>, <code>null</code> values are filtered
	 */
	public InetSocketAddressMatcherList(Collection<? extends InetSocketAddressMatcher> networks)
	{
		this(networks == null ? null : networks.stream());
	}

	/**
	 * @param networks
	 *            may be <code>null</code>, <code>null</code> values are filtered
	 */
	public InetSocketAddressMatcherList(Stream<? extends InetSocketAddressMatcher> networks)
	{
		this.networks = networks == null ? List.of()
				: networks.filter(Objects::nonNull).map(InetSocketAddressMatcher.class::cast).toList();
	}

	@Override
	public boolean matches(InetSocketAddress address)
	{
		if (address == null)
			return false;

		return networks.stream().anyMatch(n -> n.matches(address));
	}

	public List<InetSocketAddressMatcher> getNetworks()
	{
		return networks;
	}

	@Override
	public String toString()
	{
		return networks.stream().map(InetSocketAddressMatcher::toString).collect(Collectors.joining(", ", "[", "]"));
	}
}
