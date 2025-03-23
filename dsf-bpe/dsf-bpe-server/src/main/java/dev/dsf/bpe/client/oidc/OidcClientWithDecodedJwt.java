package dev.dsf.bpe.client.oidc;

import com.auth0.jwt.interfaces.DecodedJWT;

import dev.dsf.bpe.api.client.oidc.Configuration;
import dev.dsf.bpe.api.client.oidc.Jwks;
import dev.dsf.bpe.api.client.oidc.OidcClient;
import dev.dsf.bpe.api.client.oidc.OidcClientException;

public interface OidcClientWithDecodedJwt extends OidcClient
{
	DecodedJWT getAccessTokenDecoded() throws OidcClientException;

	DecodedJWT getAccessTokenDecoded(Configuration configuration, Jwks jwks) throws OidcClientException;

	@Override
	default char[] getAccessToken() throws OidcClientException
	{
		return getAccessTokenDecoded().getToken().toCharArray();
	}

	@Override
	default char[] getAccessToken(Configuration configuration, Jwks jwks) throws OidcClientException
	{
		return getAccessTokenDecoded(configuration, jwks).getToken().toCharArray();
	}
}
