package dev.dsf.common.auth;

import java.util.Map;

public interface DsfOpenIdCredentials
{
	String getUserId();

	Map<String, Object> getAccessToken();

	Map<String, Object> getIdToken();

	/**
	 * @param key
	 *            not <code>null</code>
	 * @return <code>null</code> if no {@link Long} entry with the given <b>key</b> in id-token
	 */
	Long getLongClaim(String key);

	/**
	 * @param key
	 *            not <code>null</code>
	 * @param defaultValue
	 * @return <b>defaultValue</b> if no {@link String} entry with the given <b>key</b> in id-token
	 */
	String getStringClaimOrDefault(String key, String defaultValue);
}
