package dev.dsf.common.auth.conf;

import java.security.cert.X509Certificate;

import org.eclipse.jetty.security.openid.OpenIdCredentials;

public interface IdentityProvider
{
	Identity getIdentity(OpenIdCredentials credentials);

	Identity getIdentity(X509Certificate[] certificates);
}
