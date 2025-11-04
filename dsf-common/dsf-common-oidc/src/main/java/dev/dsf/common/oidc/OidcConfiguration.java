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
package dev.dsf.common.oidc;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OidcConfiguration(@JsonProperty("issuer") String issuer,
		@JsonProperty("token_endpoint") String tokenEndpoint, @JsonProperty("jwks_uri") String jwksUri,
		@JsonProperty("grant_types_supported") Set<String> grantTypesSupported)
{
	@JsonCreator
	public OidcConfiguration(@JsonProperty("issuer") String issuer,
			@JsonProperty("token_endpoint") String tokenEndpoint, @JsonProperty("jwks_uri") String jwksUri,
			@JsonProperty("grant_types_supported") Set<String> grantTypesSupported)
	{
		this.issuer = issuer;
		this.tokenEndpoint = tokenEndpoint;
		this.jwksUri = jwksUri;

		this.grantTypesSupported = new HashSet<>();
		if (grantTypesSupported != null)
			this.grantTypesSupported.addAll(grantTypesSupported);
	}

	@Override
	public Set<String> grantTypesSupported()
	{
		return Collections.unmodifiableSet(grantTypesSupported);
	}
}
