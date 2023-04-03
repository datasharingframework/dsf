package dev.dsf.common.auth;

import java.util.Map;

import org.eclipse.jetty.security.openid.JwtDecoder;
import org.eclipse.jetty.security.openid.OpenIdCredentials;

public class DsfOpenIdCredentialsImpl implements DsfOpenIdCredentials
{
	private static final String ACCESS_TOKEN = "access_token";
	private static final String ID_TOKEN = "id_token";

	private final OpenIdCredentials credentials;

	public DsfOpenIdCredentialsImpl(OpenIdCredentials credentials)
	{
		this.credentials = credentials;
	}

	public OpenIdCredentials getCredentials()
	{
		return credentials;
	}

	@Override
	public String getUserId()
	{
		return credentials.getUserId();
	}

	@Override
	public Map<String, Object> getIdToken()
	{
		return getToken(ID_TOKEN);
	}

	@Override
	public Map<String, Object> getAccessToken()
	{
		return getToken(ACCESS_TOKEN);
	}

	private Map<String, Object> getToken(String tokenName)
	{
		String token = (String) credentials.getResponse().get(tokenName);
		return JwtDecoder.decode(token);
	}

	@Override
	public Long getLongClaim(String key)
	{
		Object o = credentials.getClaims().get(key);
		return o instanceof Long ? (Long) o : null;
	}

	@Override
	public String getStringClaimOrDefault(String key, String defaultValue)
	{
		Object o = credentials.getClaims().getOrDefault(key, defaultValue);
		return o instanceof String ? (String) o : defaultValue;
	}
}
