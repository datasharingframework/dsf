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
package dev.dsf.fhir.history;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hl7.fhir.r4.model.Resource;

public class HistoryEntry
{
	private final UUID id;
	private final String version;
	private final String resourceType;
	private final String method;
	private final LocalDateTime lastUpdated;
	private final Resource resource;

	public HistoryEntry(UUID id, String version, String resourceType, String method, LocalDateTime lastUpdated,
			Resource resource)
	{
		this.id = id;
		this.version = version;
		this.resourceType = resourceType;
		this.method = method;
		this.lastUpdated = lastUpdated;
		this.resource = resource;
	}

	public UUID getId()
	{
		return id;
	}

	public String getVersion()
	{
		return version;
	}

	public String getResourceType()
	{
		return resourceType;
	}

	public String getMethod()
	{
		return method;
	}

	public LocalDateTime getLastUpdated()
	{
		return lastUpdated;
	}

	public Resource getResource()
	{
		return resource;
	}
}