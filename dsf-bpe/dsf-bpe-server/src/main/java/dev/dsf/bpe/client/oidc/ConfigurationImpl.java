package dev.dsf.bpe.client.oidc;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import dev.dsf.bpe.api.client.oidc.Configuration;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ConfigurationImpl implements Configuration
{
	private final String issuer;
	private final String tokenEndpoint;
	private final String jwksUri;
	private final Set<String> grantTypesSupported = new HashSet<>();

	@JsonCreator
	public ConfigurationImpl(@JsonProperty("issuer") String issuer,
			@JsonProperty("token_endpoint") String tokenEndpoint, @JsonProperty("jwks_uri") String jwksUri,
			@JsonProperty("grant_types_supported") Set<String> grantTypesSupported)
	{
		this.issuer = issuer;
		this.tokenEndpoint = tokenEndpoint;
		this.jwksUri = jwksUri;

		if (grantTypesSupported != null)
			this.grantTypesSupported.addAll(grantTypesSupported);
	}

	@Override
	public String getIssuer()
	{
		return issuer;
	}

	@Override
	public String getTokenEndpoint()
	{
		return tokenEndpoint;
	}

	@Override
	public String getJwksUri()
	{
		return jwksUri;
	}

	@Override
	public Set<String> getGrantTypesSupported()
	{
		return Collections.unmodifiableSet(grantTypesSupported);
	}
}
