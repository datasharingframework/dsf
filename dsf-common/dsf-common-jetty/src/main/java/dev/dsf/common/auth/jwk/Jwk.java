package dev.dsf.common.auth.jwk;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Jwk
{
	private final Map<String, Object> attributes = new HashMap<>();

	private final String id;
	private final String type;
	private final RSAPublicKey publicKey;

	public Jwk(Map<String, Object> attributes)
	{
		this.attributes.putAll(attributes);

		id = (String) attributes.get("kid");
		type = (String) attributes.get("kty");
		publicKey = publicKeyFrom(type, attributes);
	}

	private static RSAPublicKey publicKeyFrom(String type, Map<String, Object> attributes)
	{
		if ("RSA".equals(type))
		{
			try
			{
				KeyFactory factory = KeyFactory.getInstance("RSA");
				BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode((String) attributes.get("n")));
				BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode((String) attributes.get("e")));
				return (RSAPublicKey) factory.generatePublic(new RSAPublicKeySpec(modulus, exponent));
			}
			catch (InvalidKeySpecException | NoSuchAlgorithmException e)
			{
				throw new RuntimeException(e);
			}
		}
		else
			return null;
	}

	public Map<String, Object> getAttributes()
	{
		return Collections.unmodifiableMap(attributes);
	}

	/**
	 * @return jkw.kid
	 */
	public String getId()
	{
		return id;
	}

	/**
	 * @return jwk.kty
	 */
	public String getType()
	{
		return type;
	}

	/**
	 * @return {@link RSAPublicKey} from jwk.n and jwk.e if jwk.kty = RSA
	 */
	public Optional<RSAPublicKey> getPublicKey()
	{
		return Optional.ofNullable(publicKey);
	}
}
