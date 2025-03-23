package dev.dsf.bpe.client.oidc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import dev.dsf.bpe.api.client.oidc.Jwks;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JwksImpl implements Jwks
{
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class JwksKeyImpl implements JwksKey
	{
		private final String kid;
		private final String kty;
		private final String alg;
		private final String crv;
		private final String use;
		private final String n;
		private final String e;
		private final String x;
		private final String y;

		@JsonCreator
		public JwksKeyImpl(@JsonProperty("kid") String kid, @JsonProperty("kty") String kty,
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

		public String getKid()
		{
			return kid;
		}

		public String getKty()
		{
			return kty;
		}

		public String getAlg()
		{
			return alg;
		}

		public String getCrv()
		{
			return crv;
		}

		public String getUse()
		{
			return use;
		}

		public String getN()
		{
			return n;
		}

		public String getE()
		{
			return e;
		}

		public String getX()
		{
			return x;
		}

		public String getY()
		{
			return y;
		}
	}

	private final Map<String, JwksKey> keysByKid = new HashMap<>();

	@JsonCreator
	public JwksImpl(@JsonProperty("keys") List<JwksKeyImpl> keys)
	{
		if (keys != null)
			keysByKid.putAll(keys.stream().collect(Collectors.toMap(JwksKey::getKid, Function.identity())));
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
