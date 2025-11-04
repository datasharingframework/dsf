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
package dev.dsf.bpe.api.config;

import java.util.List;

public interface BpeProxyConfig
{
	/**
	 * @return may be <code>null</code>
	 */
	String getUrl();

	/**
	 * @return <code>true</code> if a proxy url is configured and '*' is not set as a no-proxy url
	 */
	boolean isEnabled();

	/**
	 * @param targetUrl
	 *            may be <code>null</code>
	 * @return <code>true</code> if a proxy url is configured, '*' is not set as a no-proxy url and the given
	 *         <b>targetUrl</b> is not set as a no-proxy url, <code>false</code> if the given <b>targetUrl</b> is
	 *         <code>null</code> or blank
	 * @see #getNoProxyUrls()
	 * @see String#isBlank()
	 */
	boolean isEnabled(String targetUrl);

	/**
	 * @return may be <code>null</code>
	 */
	String getUsername();

	/**
	 * @return may be <code>null</code>
	 */
	char[] getPassword();

	/**
	 * @return never <code>null</code>, may be empty
	 */
	List<String> getNoProxyUrls();

	/**
	 * Returns <code>true</code> if the given <b>targetUrl</b> is not <code>null</code> and the domain + port of the
	 * given <b>targetUrl</b> is configured as a no-proxy URL based on the environment configuration.
	 * <p>
	 * Configured no-proxy URLs are matched exactly and against sub-domains. If a port is configured, only URLs with the
	 * same port (or default port) return a <code>true</code> result.
	 * <p>
	 * <table>
	 * <caption>No-Proxy URL examples</caption>
	 * <tr>
	 * <th>Configured</th>
	 * <th>Given</th>
	 * <th>Result</th>
	 * </tr>
	 * <tr>
	 * <td>foo.bar, test.com:8080</td>
	 * <td>https://foo.bar/fhir</td>
	 * <td><i>true</i></td>
	 * </tr>
	 * <tr>
	 * <td>foo.bar, test.com:8080</td>
	 * <td>https://baz.foo.bar/test</td>
	 * <td><i>true</i></td>
	 * </tr>
	 * <tr>
	 * <td>foo.bar, test.com:8080</td>
	 * <td>https://test.com:8080/fhir</td>
	 * <td><i>true</i></td>
	 * </tr>
	 * <tr>
	 * <td>foo.bar, test.com:8080</td>
	 * <td>https://test.com/fhir</td>
	 * <td><i>false</i></td>
	 * </tr>
	 * <tr>
	 * <td>foo.bar:443</td>
	 * <td>https://foo.bar/fhir</td>
	 * <td><i>true</i></td>
	 * </tr>
	 * </table>
	 *
	 * @param targetUrl
	 *            may be <code>null</code>
	 * @return true if the given <b>targetUrl</b> is not <code>null</code> and is configured as a no-proxy url
	 */
	boolean isNoProxyUrl(String targetUrl);
}
