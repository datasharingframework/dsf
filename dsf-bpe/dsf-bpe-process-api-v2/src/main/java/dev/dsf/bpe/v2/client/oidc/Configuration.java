package dev.dsf.bpe.v2.client.oidc;

import java.util.Set;

public interface Configuration
{
	String getIssuer();

	String getTokenEndpoint();

	String getJwksUri();

	Set<String> getGrantTypesSupported();
}
