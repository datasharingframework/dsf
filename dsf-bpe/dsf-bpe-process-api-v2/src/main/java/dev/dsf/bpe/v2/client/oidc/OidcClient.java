package dev.dsf.bpe.v2.client.oidc;

/**
 * Client Credentials Grant implementation to receive access tokens from an OIDC provider.
 */
public interface OidcClient
{
	/**
	 * @return OIDC {@link Configuration} resource
	 * @throws OidcClientException
	 *             if response status not 200 OK, response issuer not matching base-url or response supported grant
	 *             types does not include <code>"client_credentials"</code>
	 */
	Configuration getConfiguration() throws OidcClientException;

	/**
	 * @return {@link Jwks} resource
	 * @throws OidcClientException
	 *             if response status not 200 OK
	 */
	Jwks getJwks() throws OidcClientException;

	/**
	 * @param configuration
	 *            not <code>null</code>
	 * @return {@link Jwks} resource
	 * @throws OidcClientException
	 *             if response status not 200 OK
	 */
	Jwks getJwks(Configuration configuration) throws OidcClientException;

	/**
	 * @return access token
	 */
	char[] getAccessToken() throws OidcClientException;

	/**
	 * @param configuration
	 *            not <code>null</code>
	 * @param jwks
	 *            not <code>null</code>
	 * @return access token
	 * @throws OidcClientException
	 *             if response status not 200 OK, OIDC provider does not support client credentials grant (Keycloak:
	 *             service accounts roles) or returned access token could not be verified
	 */
	char[] getAccessToken(Configuration configuration, Jwks jwks) throws OidcClientException;
}