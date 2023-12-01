package dev.dsf.common.auth;

import java.util.Collections;
import java.util.Map;

import org.eclipse.jetty.security.openid.JwtDecoder;
import org.eclipse.jetty.security.openid.OpenIdCredentials;

public class DsfOpenIdCredentialsImpl implements DsfOpenIdCredentials
{
	private static final String ACCESS_TOKEN = "access_token";
	private static final String ID_TOKEN = "id_token";

	private final Map<String, Object> idToken;
	private final Map<String, Object> accessToken;

	public DsfOpenIdCredentialsImpl(OpenIdCredentials credentials)
	{
		this.idToken = JwtDecoder.decode((String) credentials.getResponse().get(ID_TOKEN));
		this.accessToken = JwtDecoder.decode((String) credentials.getResponse().get(ACCESS_TOKEN));
	}

	public DsfOpenIdCredentialsImpl(String accessToken)
	{
		this.idToken = Collections.emptyMap();
		this.accessToken = JwtDecoder.decode(accessToken);
	}

	@Override
	public String getUserId()
	{
		return (String) accessToken.get("sub");
	}

	@Override
	public Map<String, Object> getIdToken()
	{
		return Collections.unmodifiableMap(idToken);
	}

	@Override
	public Map<String, Object> getAccessToken()
	{
		return Collections.unmodifiableMap(accessToken);
	}

	@Override
	public Long getLongClaim(String key)
	{
		Object o = getAccessToken().get(key);
		return o instanceof Long l ? l : null;
	}

	@Override
	public String getStringClaimOrDefault(String key, String defaultValue)
	{
		Object o = getAccessToken().getOrDefault(key, defaultValue);
		return o instanceof String s ? s : defaultValue;
	}
}
