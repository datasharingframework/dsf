package dev.dsf.bpe.v2.client.oidc;

import java.util.Optional;
import java.util.Set;

public interface Jwks
{
	public static interface JwksKey
	{
		String getKid();

		String getKty();

		String getAlg();

		String getCrv();

		String getUse();

		String getN();

		String getE();

		String getX();

		String getY();
	}

	Set<JwksKey> getKeys();

	Optional<JwksKey> getKey(String kid);
}
