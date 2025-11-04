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
package dev.dsf.bpe.v1.variables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TargetsImpl implements Targets
{
	private final List<TargetImpl> entries = new ArrayList<>();

	@JsonCreator
	public TargetsImpl(@JsonProperty("entries") List<? extends TargetImpl> targets)
	{
		if (targets != null)
			this.entries.addAll(targets);
	}

	@JsonProperty("entries")
	@Override
	public List<Target> getEntries()
	{
		return Collections.unmodifiableList(entries);
	}

	@Override
	public Targets removeByEndpointIdentifierValue(Target target)
	{
		if (target == null)
			return new TargetsImpl(entries);

		return removeByEndpointIdentifierValue(target.getEndpointIdentifierValue());
	}

	@Override
	public Targets removeByEndpointIdentifierValue(String targetEndpointIdentifierValue)
	{
		if (targetEndpointIdentifierValue == null)
			return new TargetsImpl(entries);

		return new TargetsImpl(
				entries.stream().filter(t -> !targetEndpointIdentifierValue.equals(t.getEndpointIdentifierValue()))
						.collect(Collectors.toList()));
	}

	@Override
	public Targets removeAllByEndpointIdentifierValue(Collection<String> targetEndpointIdentifierValues)
	{
		if (targetEndpointIdentifierValues == null || targetEndpointIdentifierValues.isEmpty())
			return new TargetsImpl(entries);

		return new TargetsImpl(
				entries.stream().filter(t -> !targetEndpointIdentifierValues.contains(t.getEndpointIdentifierValue()))
						.collect(Collectors.toList()));
	}

	@JsonIgnore
	@Override
	public boolean isEmpty()
	{
		return entries.isEmpty();
	}

	@Override
	public String toString()
	{
		return "TargetsImpl [entries=" + entries + "]";
	}
}
