package dev.dsf.bpe.api.client.oidc;

import java.util.Set;

public interface Configuration
{
	String getIssuer();

	String getTokenEndpoint();

	String getJwksUri();

	Set<String> getGrantTypesSupported();
}
