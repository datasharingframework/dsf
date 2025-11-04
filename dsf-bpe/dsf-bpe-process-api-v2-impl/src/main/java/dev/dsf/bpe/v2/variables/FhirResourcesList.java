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
package dev.dsf.bpe.v2.variables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FhirResourcesList
{
	private final List<Resource> resources = new ArrayList<>();

	@JsonCreator
	public FhirResourcesList(@JsonProperty("resources") Collection<? extends Resource> resources)
	{
		if (resources != null)
			this.resources.addAll(resources);
	}

	public FhirResourcesList(Resource... resources)
	{
		this(List.of(resources));
	}

	@JsonProperty("resources")
	public List<Resource> getResources()
	{
		return Collections.unmodifiableList(resources);
	}

	@SuppressWarnings("unchecked")
	@JsonIgnore
	public <R extends Resource> List<R> getResourcesAndCast()
	{
		return (List<R>) getResources();
	}

	@Override
	public String toString()
	{
		return "FhirResourcesList" + resources.stream().map(r -> r.getIdElement().toUnqualified().getValue())
				.collect(Collectors.joining(", ", "[", "]"));
	}
}
