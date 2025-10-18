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
