package dev.dsf.common.auth.conf;

import java.security.cert.X509Certificate;

import dev.dsf.common.auth.DsfOpenIdCredentials;

public interface IdentityProvider
{
	Identity getIdentity(DsfOpenIdCredentials credentials);

	Identity getIdentity(X509Certificate[] certificates);
}
