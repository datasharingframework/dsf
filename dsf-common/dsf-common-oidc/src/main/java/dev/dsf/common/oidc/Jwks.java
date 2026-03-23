/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.common.oidc;

import java.math.BigInteger;
import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.ECDSAKeyProvider;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Jwks
{
	private static final Logger logger = LoggerFactory.getLogger(Jwks.class);

	private static final int RSA_MIN_KEY_LENGTH = 2048;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static record JwksKey(@JsonProperty("kid") String kid, @JsonProperty("kty") String kty,
			@JsonProperty("alg") String alg, @JsonProperty("crv") String crv, @JsonProperty("use") String use,
			@JsonProperty("n") String n, @JsonProperty("e") String e, @JsonProperty("x") String x,
			@JsonProperty("y") String y)
	{
		@JsonCreator
		public JwksKey(@JsonProperty("kid") String kid, @JsonProperty("kty") String kty,
				@JsonProperty("alg") String alg, @JsonProperty("crv") String crv, @JsonProperty("use") String use,
				@JsonProperty("n") String n, @JsonProperty("e") String e, @JsonProperty("x") String x,
				@JsonProperty("y") String y)
		{
			this.kid = kid;
			this.kty = kty;
			this.alg = alg;
			this.crv = crv;
			this.use = use;
			this.n = n;
			this.e = e;
			this.x = x;
			this.y = y;
		}

		/**
		 * @return algorithm for the enclosed key material
		 * @throws JwksException
		 *             if {@link Algorithm} can't be created or is not supported for the enclosed key material
		 */
		public Optional<Algorithm> toAlgorithm() throws JwksException
		{
			return Optional.ofNullable(switch (kty)
			{
				case "RSA" -> toRsaAlgorithm();
				case "EC" -> toEcdsaAlgorithm();

				default -> {
					logger.warn("JWKS kty property value '{}' not one of 'RSA' or 'EC'", kty);
					yield null;
				}
			});
		}

		private Algorithm toRsaAlgorithm()
		{
			RSAPublicKey key = toRsaPublicKey(n, e);

			if (key == null)
				return null;

			RSAKeyProvider keyProvider = toRsaKeyProvider(key, kid);

			return switch (alg)
			{
				case "RS256" -> Algorithm.RSA256(keyProvider);
				case "RS384" -> Algorithm.RSA384(keyProvider);
				case "RS512" -> Algorithm.RSA512(keyProvider);

				default -> {
					logger.warn("JWKS alg property value '{}' not one of 'RS256', 'RS384' or 'RS512'", alg);
					yield null;
				}
			};
		}

		private Algorithm toEcdsaAlgorithm()
		{
			ECPublicKey key = toEcPublicKey(x, y, crv);

			if (key == null)
				return null;

			ECDSAKeyProvider keyProvider = toEcKeyProvider(key, kid);

			return switch (alg)
			{
				case "ES256" -> Algorithm.ECDSA256(keyProvider);
				case "ES384" -> Algorithm.ECDSA384(keyProvider);
				case "ES512" -> Algorithm.ECDSA512(keyProvider);

				default -> {
					logger.warn("JWKS crv property value '{}' not one of 'ES256', 'ES384' or 'ES512'", alg);
					yield null;
				}
			};
		}

		private RSAKeyProvider toRsaKeyProvider(RSAPublicKey key, String kid)
		{
			return new RSAKeyProvider()
			{
				@Override
				public RSAPublicKey getPublicKeyById(String keyId)
				{
					if (kid != null && kid.equals(keyId))
						return key;
					else
						return null;
				}

				@Override
				public String getPrivateKeyId()
				{
					return null;
				}

				@Override
				public RSAPrivateKey getPrivateKey()
				{
					return null;
				}
			};
		}

		private RSAPublicKey toRsaPublicKey(String n, String e)
		{
			BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
			BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));

			if (modulus.bitLength() < RSA_MIN_KEY_LENGTH)
			{
				logger.warn("JWKS RSA key (kid: '{}') length {} <{} bit", kid, modulus.bitLength(), RSA_MIN_KEY_LENGTH);
				return null;
			}

			try
			{
				RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);

				KeyFactory factory = KeyFactory.getInstance("RSA");
				return (RSAPublicKey) factory.generatePublic(keySpec);
			}
			catch (InvalidKeySpecException | NoSuchAlgorithmException ex)
			{
				logger.debug("Unable to create RSA public key: {} - {}", ex.getClass().getName(), ex.getMessage());
				logger.warn("Unable to create RSA public key", ex);

				return null;
			}
		}

		private ECDSAKeyProvider toEcKeyProvider(ECPublicKey key, String kid)
		{
			return new ECDSAKeyProvider()
			{
				@Override
				public ECPublicKey getPublicKeyById(String keyId)
				{
					if (kid != null && kid.equals(keyId))
						return key;
					else
						return null;
				}

				@Override
				public String getPrivateKeyId()
				{
					return null;
				}

				@Override
				public ECPrivateKey getPrivateKey()
				{
					return null;
				}
			};
		}

		private ECPublicKey toEcPublicKey(String x, String y, String crv)
		{
			BigInteger xCoordinate = new BigInteger(1, Base64.getUrlDecoder().decode(x));
			BigInteger yCoordinate = new BigInteger(1, Base64.getUrlDecoder().decode(y));

			return switch (crv)
			{
				case "P-256" -> toEcPublicKey(xCoordinate, yCoordinate, new ECGenParameterSpec("secp256r1"));
				case "P-384" -> toEcPublicKey(xCoordinate, yCoordinate, new ECGenParameterSpec("secp384r1"));
				case "P-521" -> toEcPublicKey(xCoordinate, yCoordinate, new ECGenParameterSpec("secp521r1"));

				default -> {
					logger.warn("JWKS crv property value '{}' not one of 'P-256', 'P-384' or 'P-521'", crv);
					yield null;
				}
			};
		}

		private ECPublicKey toEcPublicKey(BigInteger xCoordinate, BigInteger yCoordinate, ECGenParameterSpec curve)
		{
			try
			{
				AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
				parameters.init(curve);
				ECParameterSpec ecParameters = parameters.getParameterSpec(ECParameterSpec.class);
				ECPublicKeySpec keySpec = new ECPublicKeySpec(new ECPoint(xCoordinate, yCoordinate), ecParameters);

				KeyFactory factory = KeyFactory.getInstance("EC");
				return (ECPublicKey) factory.generatePublic(keySpec);
			}
			catch (NoSuchAlgorithmException | InvalidParameterSpecException | InvalidKeySpecException ex)
			{
				logger.debug("Unable to create EC public key", ex);
				logger.warn("Unable to create EC public key: {} - {}", ex.getClass().getName(), ex.getMessage());

				return null;
			}
		}
	}

	private final Map<String, JwksKey> keysByKid = new HashMap<>();

	@JsonCreator
	public Jwks(@JsonProperty("keys") List<JwksKey> keys)
	{
		if (keys != null)
			keysByKid.putAll(keys.stream().collect(Collectors.toMap(JwksKey::kid, Function.identity())));
	}


	public Set<JwksKey> getKeys()
	{
		return Set.copyOf(keysByKid.values());
	}


	public Optional<JwksKey> getKey(String kid)
	{
		return Optional.ofNullable(keysByKid.get(kid));
	}
}
