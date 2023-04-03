package dev.dsf.common.auth.jwk;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jetty.util.ajax.JSON;
import org.eclipse.jetty.util.ajax.JSON.StringSource;

public class Jwks
{
	private final Map<String, Jwk> jwks;

	@SuppressWarnings("unchecked")
	public Jwks(Map<String, Object> json)
	{
		Object[] keys = (Object[]) json.get("keys");
		jwks = Arrays.stream(keys).filter(o -> o instanceof Map).map(o -> (Map<String, Object>) o).map(Jwk::new)
				.collect(Collectors.toMap(Jwk::getId, Function.identity(), (v1, v2) ->
				{
					throw new IllegalStateException("duplicate key id " + v1.getId());
				}, LinkedHashMap::new));
	}

	@SuppressWarnings("unchecked")
	public static Jwks from(String json)
	{
		Object jwksJson = new JSON().parse(new StringSource(json));
		if (jwksJson instanceof Map)
			return new Jwks((Map<String, Object>) jwksJson);
		else
			return new Jwks(Collections.emptyMap());
	}

	public Jwk getKey(String id)
	{
		return jwks.get(id);
	}

	public List<Jwk> getAllKeys()
	{
		return new ArrayList<>(jwks.values());
	}
}
