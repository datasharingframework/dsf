package dev.dsf.bpe.client.oidc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TokenResult
{
	private final String accessToken;
	private final int expiresIn;

	@JsonCreator
	public TokenResult(@JsonProperty("access_token") String accessToken, @JsonProperty("expires_in") int expiresIn)
	{
		this.accessToken = accessToken;
		this.expiresIn = expiresIn;
	}

	public String getAccessToken()
	{
		return accessToken;
	}

	public int getExpiresIn()
	{
		return expiresIn;
	}
}