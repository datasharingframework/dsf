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
package dev.dsf.common.auth;

import java.util.Map;

public interface DsfOpenIdCredentials
{
	String getUserId();

	Map<String, Object> getAccessToken();

	/**
	 * @return empty when authentication via bearer token
	 */
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
