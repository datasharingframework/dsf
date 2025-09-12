package dev.dsf.common.oidc;

import jakarta.ws.rs.core.Response.Status;

public interface BaseOidcClient
{
	/**
	 * @return OIDC {@link OidcConfiguration} resource
	 * @throws OidcClientException
	 *             if response status not {@link Status#OK}, response issuer not matching base-url or response supported
	 *             grant types does not include <code>"client_credentials"</code>
	 */
	OidcConfiguration getConfiguration() throws OidcClientException;

	/**
	 * @return {@link Jwks} resource
	 * @throws OidcClientException
	 *             if response status not {@link Status#OK}
	 */
	Jwks getJwks() throws OidcClientException;

	/**
	 * @param configuration
	 *            not <code>null</code>
	 * @return {@link Jwks} resource
	 * @throws OidcClientException
	 *             if response status not {@link Status#OK}
	 */
	Jwks getJwks(OidcConfiguration configuration) throws OidcClientException;
}
