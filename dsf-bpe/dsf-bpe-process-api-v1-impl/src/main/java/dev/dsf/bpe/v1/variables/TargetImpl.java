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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TargetImpl implements Target
{
	private final String organizationIdentifierValue;
	private final String endpointIdentifierValue;
	private final String endpointUrl;
	private final String correlationKey;

	@JsonCreator
	public TargetImpl(
			@JsonProperty("organizationIdentifierValue") @JsonAlias("targetOrganizationIdentifierValue") String organizationIdentifierValue,
			@JsonProperty("endpointIdentifierValue") String endpointIdentifierValue,
			@JsonProperty("endpointUrl") @JsonAlias("targetEndpointUrl") String endpointUrl,
			@JsonProperty("correlationKey") String correlationKey)
	{
		this.organizationIdentifierValue = organizationIdentifierValue;
		this.endpointIdentifierValue = endpointIdentifierValue;
		this.endpointUrl = endpointUrl;
		this.correlationKey = correlationKey;
	}

	@Override
	@JsonProperty("organizationIdentifierValue")
	public String getOrganizationIdentifierValue()
	{
		return organizationIdentifierValue;
	}

	@Override
	@JsonProperty("endpointIdentifierValue")
	public String getEndpointIdentifierValue()
	{
		return endpointIdentifierValue;
	}

	@Override
	@JsonProperty("endpointUrl")
	public String getEndpointUrl()
	{
		return endpointUrl;
	}

	@Override
	@JsonProperty("correlationKey")
	public String getCorrelationKey()
	{
		return correlationKey;
	}

	@Override
	public String toString()
	{
		return "TargetImpl [organizationIdentifierValue=" + organizationIdentifierValue + ", endpointIdentifierValue="
				+ endpointIdentifierValue + ", endpointUrl=" + endpointUrl + ", correlationKey=" + correlationKey + "]";
	}
}
