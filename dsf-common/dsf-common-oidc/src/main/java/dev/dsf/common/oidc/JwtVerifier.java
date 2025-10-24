package dev.dsf.common.oidc;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;

public interface JwtVerifier
{
	DecodedJWT verifyBackchannelLogout(String token) throws JWTVerificationException, OidcClientException;

	DecodedJWT verifyBearerToken(String token) throws JWTVerificationException, OidcClientException;
}
