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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BaseJwtVerifierTest
{
	private static final String ISSUER = "https://issuer.example.com";
	private static final String TOKEN_TYPE = "Bearer";
	private static final String KEY_ID = "p384-test-key";
	private static final String SUBJECT = "alice";

	private static final Duration LEEWAY = Duration.ofSeconds(30);

	private static final ObjectMapper objectMapper = new ObjectMapper();

	private KeyPair keyPair;
	private ECPublicKey publicKey;
	private ECPrivateKey privateKey;

	private BaseJwtVerifier verifier;

	@Before
	public void setUp() throws Exception
	{
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(new ECGenParameterSpec("secp384r1"));

		keyPair = generator.generateKeyPair();

		publicKey = (ECPublicKey) keyPair.getPublic();
		privateKey = (ECPrivateKey) keyPair.getPrivate();

		Jwks jwks = loadJwks(createJwksJson(publicKey));

		verifier = new BaseJwtVerifierImpl(TOKEN_TYPE, ISSUER, () -> jwks, LEEWAY);
	}

	private static Jwks loadJwks(String jwksJson) throws JsonMappingException, JsonProcessingException
	{
		return objectMapper.readValue(jwksJson, Jwks.class);
	}

	private static String createJwksJson(ECPublicKey publicKey)
	{
		return """
				{
				  "keys": [
				    {
				      "kty": "EC",
				      "use": "sig",
				      "key_ops": ["verify"],
				      "alg": "ES384",
				      "crv": "P-384",
				      "kid": "%s",
				      "x": "%s",
				      "y": "%s"
				    }
				  ]
				}
				""".formatted(KEY_ID, encodeCoordinate(publicKey.getW().getAffineX()),
				encodeCoordinate(publicKey.getW().getAffineY()));
	}

	private static String encodeCoordinate(BigInteger coordinate)
	{
		byte[] bytes = coordinate.toByteArray();
		byte[] fixed = new byte[48];

		int sourceOffset = bytes.length > 48 ? bytes.length - 48 : 0;
		int length = bytes.length - sourceOffset;

		System.arraycopy(bytes, sourceOffset, fixed, 48 - length, length);

		return Base64.getUrlEncoder().withoutPadding().encodeToString(fixed);
	}

	@Test
	public void shouldVerifyJwtSignedWithP384Key() throws Exception
	{
		Instant now = Instant.now();

		String token = JWT.create().withKeyId(KEY_ID).withIssuer(ISSUER).withSubject(SUBJECT)
				.withIssuedAt(Date.from(now.minusSeconds(10))).withNotBefore(Date.from(now.minusSeconds(5)))
				.withExpiresAt(Date.from(now.plusSeconds(300))).sign(Algorithm.ECDSA384(null, privateKey));

		JWTVerifier jwtVerifier = verifier.createVerification(token).build();

		DecodedJWT decoded = jwtVerifier.verify(token);

		assertEquals(ISSUER, decoded.getIssuer());
		assertEquals(SUBJECT, decoded.getSubject());
		assertEquals(KEY_ID, decoded.getKeyId());
		assertEquals("ES384", decoded.getAlgorithm());
	}

	@Test
	public void shouldRejectJwtSignedWithDifferentP384Key() throws Exception
	{
		KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
		generator.initialize(new ECGenParameterSpec("secp384r1"));
		KeyPair differentKeyPair = generator.generateKeyPair();

		Instant now = Instant.now();

		String token = JWT.create().withKeyId(KEY_ID).withIssuer(ISSUER).withSubject(SUBJECT)
				.withIssuedAt(Date.from(now.minusSeconds(10))).withNotBefore(Date.from(now.minusSeconds(5)))
				.withExpiresAt(Date.from(now.plusSeconds(300)))
				.sign(Algorithm.ECDSA384(null, (ECPrivateKey) differentKeyPair.getPrivate()));

		JWTVerifier jwtVerifier = verifier.createVerification(token).build();

		assertThrows(JWTVerificationException.class, () -> jwtVerifier.verify(token));
	}

	@Test
	public void shouldRejectWrongIssuer() throws Exception
	{
		Instant now = Instant.now();

		String token = JWT.create().withKeyId(KEY_ID).withIssuer("https://wrong-issuer.example.com")
				.withSubject(SUBJECT).withIssuedAt(Date.from(now.minusSeconds(10)))
				.withNotBefore(Date.from(now.minusSeconds(5))).withExpiresAt(Date.from(now.plusSeconds(300)))
				.sign(Algorithm.ECDSA384(null, privateKey));

		JWTVerifier jwtVerifier = verifier.createVerification(token).build();

		assertThrows(JWTVerificationException.class, () -> jwtVerifier.verify(token));
	}

	@Test
	public void shouldRejectExpiredTokenBeyondLeeway() throws Exception
	{
		Instant now = Instant.now();

		String token = JWT.create().withKeyId(KEY_ID).withIssuer(ISSUER).withSubject(SUBJECT)
				.withIssuedAt(Date.from(now.minusSeconds(120))).withNotBefore(Date.from(now.minusSeconds(120)))
				.withExpiresAt(Date.from(now.minusSeconds(60))).sign(Algorithm.ECDSA384(null, privateKey));

		JWTVerifier jwtVerifier = verifier.createVerification(token).build();

		assertThrows(JWTVerificationException.class, () -> jwtVerifier.verify(token));
	}

	private static KeyPair generateRsaKeyPair() throws Exception
	{
		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(2048);
		return generator.generateKeyPair();
	}

	private static String encodeUnsigned(BigInteger value)
	{
		byte[] bytes = value.toByteArray();

		if (bytes.length > 1 && bytes[0] == 0)
			bytes = Arrays.copyOfRange(bytes, 1, bytes.length);

		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private static String rsaJwk(KeyPair pair, String alg, String use, List<String> keyOps)
	{
		RSAPublicKey publicKey = (RSAPublicKey) pair.getPublic();

		String operations = keyOps.stream().map(op -> "\"" + op + "\"").collect(Collectors.joining(","));

		return """
				{
				  "kty": "RSA",
				  "kid": "test-key",
				  "alg": "%s",
				  "use": "%s",
				  "key_ops": [%s],
				  "n": "%s",
				  "e": "%s"
				}
				""".formatted(alg, use, operations, encodeUnsigned(publicKey.getModulus()),
				encodeUnsigned(publicKey.getPublicExponent()));
	}

	private static String jwksDocument(String jwk)
	{
		return """
				{"keys": [%s]}
				""".formatted(jwk);
	}

	private static String createTokenWithHeaderAlgorithm(String alg)
	{
		String header = """
				{"alg":"%s","typ":"JWT","kid":"test-key"}
				""".formatted(alg).trim();

		String payload = """
				{"iss":"https://issuer.example.com","sub":"alice"}
				""".trim();

		Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();

		return encoder.encodeToString(header.getBytes(StandardCharsets.UTF_8)) + "."
				+ encoder.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "."
				+ Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[] { 1, 2, 3 });
	}

	private BaseJwtVerifierImpl verifierFor(String jwksJson) throws JsonMappingException, JsonProcessingException
	{
		Jwks jwks = loadJwks(jwksJson);

		return new BaseJwtVerifierImpl(TOKEN_TYPE, "https://issuer.example.com", () -> jwks, Duration.ofSeconds(30));
	}

	@Test
	public void shouldRejectHmacAlgorithm() throws Exception
	{
		KeyPair rsa = generateRsaKeyPair();

		String jwks = jwksDocument(rsaJwk(rsa, "RS256", "sig", List.of("verify")));

		BaseJwtVerifierImpl verifier = verifierFor(jwks);

		assertThrows(OidcClientException.class,
				() -> verifier.createVerification(createTokenWithHeaderAlgorithm("HS256")));
	}

	@Test
	public void shouldRejectNoneAlgorithm() throws Exception
	{
		KeyPair rsa = generateRsaKeyPair();

		BaseJwtVerifierImpl verifier = verifierFor(jwksDocument(rsaJwk(rsa, "RS256", "sig", List.of("verify"))));

		assertThrows(OidcClientException.class,
				() -> verifier.createVerification(createTokenWithHeaderAlgorithm("none")));
	}

	@Test
	public void shouldRejectUnsupportedPssAlgorithm() throws Exception
	{
		KeyPair rsa = generateRsaKeyPair();

		BaseJwtVerifierImpl verifier = verifierFor(jwksDocument(rsaJwk(rsa, "RS256", "sig", List.of("verify"))));

		assertThrows(OidcClientException.class,
				() -> verifier.createVerification(createTokenWithHeaderAlgorithm("PS256")));
	}

	@Test
	public void shouldRejectSigningOnlyKey() throws Exception
	{
		KeyPair rsa = generateRsaKeyPair();

		String jwks = jwksDocument(rsaJwk(rsa, "RS256", "sig", List.of("sign")));

		BaseJwtVerifierImpl verifier = verifierFor(jwks);

		OidcClientException exception = assertThrows(OidcClientException.class,
				() -> verifier.createVerification(createTokenWithHeaderAlgorithm("RS256")));

		assertEquals(TOKEN_TYPE + " token key with use 'sig' or key_ops 'verify' not in JWKS", exception.getMessage());
	}

	@Test
	public void shouldRejectEncryptionOnlyKey() throws Exception
	{
		KeyPair rsa = generateRsaKeyPair();

		String jwks = jwksDocument(rsaJwk(rsa, "RS256", "enc", List.of("verify")));

		BaseJwtVerifierImpl verifier = verifierFor(jwks);

		OidcClientException exception = assertThrows(OidcClientException.class,
				() -> verifier.createVerification(createTokenWithHeaderAlgorithm("RS256")));

		assertEquals(TOKEN_TYPE + " token key with use 'sig' or key_ops 'verify' not in JWKS", exception.getMessage());
	}

	@Test
	public void shouldRejectContradictoryKeyMetadata() throws Exception
	{
		KeyPair rsa = generateRsaKeyPair();

		// "use" allows signatures, but key_ops permits signing only.
		String jwks = jwksDocument(rsaJwk(rsa, "RS256", "sig", List.of("sign")));

		BaseJwtVerifierImpl verifier = verifierFor(jwks);

		assertThrows(OidcClientException.class,
				() -> verifier.createVerification(createTokenWithHeaderAlgorithm("RS256")));
	}

	@Test
	public void shouldAcceptRsaKeyWithoutAlgWhenTokenUsesRs256() throws Exception
	{
		KeyPair pair = generateRsaKeyPair();

		String jwk = """
				{
				  "kty": "RSA",
				  "kid": "test-key",
				  "use": "sig",
				  "key_ops": ["verify"],
				  "n": "%s",
				  "e": "%s"
				}
				""".formatted(encodeUnsigned(((RSAPublicKey) pair.getPublic()).getModulus()),
				encodeUnsigned(((RSAPublicKey) pair.getPublic()).getPublicExponent()));

		BaseJwtVerifierImpl verifier = verifierFor(jwksDocument(jwk));

		Instant now = Instant.now();

		String token = JWT.create().withKeyId("test-key").withIssuer("https://issuer.example.com").withSubject("alice")
				.withIssuedAt(Date.from(now.minusSeconds(10))).withNotBefore(Date.from(now.minusSeconds(5)))
				.withExpiresAt(Date.from(now.plusSeconds(300)))
				.sign(Algorithm.RSA256(null, (RSAPrivateKey) pair.getPrivate()));

		DecodedJWT decoded = verifier.createVerification(token).build().verify(token);

		assertEquals("RS256", decoded.getAlgorithm());
		assertEquals("alice", decoded.getSubject());
	}

	@Test
	public void shouldRejectUnsupportedAlgorithmWhenJwkHasNoAlg() throws Exception
	{
		KeyPair pair = generateRsaKeyPair();

		String jwk = """
				{
				  "kty": "RSA",
				  "kid": "test-key",
				  "use": "sig",
				  "key_ops": ["verify"],
				  "n": "%s",
				  "e": "%s"
				}
				""".formatted(encodeUnsigned(((RSAPublicKey) pair.getPublic()).getModulus()),
				encodeUnsigned(((RSAPublicKey) pair.getPublic()).getPublicExponent()));

		BaseJwtVerifierImpl verifier = verifierFor(jwksDocument(jwk));

		String token = createTokenWithHeaderAlgorithm("HS256");

		OidcClientException exception = assertThrows(OidcClientException.class,
				() -> verifier.createVerification(token));

		assertEquals(TOKEN_TYPE + " token key has unsupported type (kty) / algorithm (alg) / key-size",
				exception.getMessage());
	}

	@Test
	public void shouldRejectEcAlgorithmForRsaKeyWithoutAlg() throws Exception
	{
		KeyPair pair = generateRsaKeyPair();

		String jwk = """
				{
				  "kty": "RSA",
				  "kid": "test-key",
				  "use": "sig",
				  "key_ops": ["verify"],
				  "n": "%s",
				  "e": "%s"
				}
				""".formatted(encodeUnsigned(((RSAPublicKey) pair.getPublic()).getModulus()),
				encodeUnsigned(((RSAPublicKey) pair.getPublic()).getPublicExponent()));

		BaseJwtVerifierImpl verifier = verifierFor(jwksDocument(jwk));

		String token = createTokenWithHeaderAlgorithm("ES384");

		OidcClientException exception = assertThrows(OidcClientException.class,
				() -> verifier.createVerification(token));

		assertEquals(TOKEN_TYPE + " token key has unsupported type (kty) / algorithm (alg) / key-size",
				exception.getMessage());
	}

	@Test
	public void shouldRejectTokenWhenJwksContainsNoKeys() throws JsonMappingException, JsonProcessingException
	{
		Jwks jwks = loadJwks("{\"keys\": []}");

		BaseJwtVerifierImpl verifier = new BaseJwtVerifierImpl(TOKEN_TYPE, "https://issuer.example.com", () -> jwks,
				Duration.ofSeconds(30));

		String token = createTokenWithHeaderAlgorithm("RS256");

		OidcClientException exception = assertThrows(OidcClientException.class,
				() -> verifier.createVerification(token));

		assertEquals(TOKEN_TYPE + " token key not in JWKS", exception.getMessage());
	}
}