package dev.dsf.common.auth;

import java.security.cert.X509Certificate;

public interface IdentityProvider
{
	Identity getIdentity(String jwtToken);

	Identity getIdentity(X509Certificate[] certificates);
}
