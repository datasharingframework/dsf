package dev.dsf.bpe.v1.config;

import java.util.List;

public interface ProxyConfig
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
	 * Returns <code>true</code> if the given <b>url</b> is not <code>null</code> and the domain + port of the given
	 * <b>url</b> is configured as a no-proxy URL based on the environment configuration.
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
	 * @param url
	 *            may be <code>null</code>
	 * @return true if the given <b>url</b> is not <code>null</code> and is configured as a no-proxy url
	 */
	boolean isNoProxyUrl(String url);
}
