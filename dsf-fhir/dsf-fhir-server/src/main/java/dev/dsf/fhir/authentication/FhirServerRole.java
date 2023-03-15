package dev.dsf.fhir.authentication;

import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

import dev.dsf.common.auth.conf.Role;

public enum FhirServerRole implements Role
{
	CREATE, READ, UPDATE, DELETE, SEARCH, HISTORY, PERMANENT_DELETE, WEBSOCKET;

	public static final Set<FhirServerRole> LOCAL_ORGANIZATION = EnumSet.of(CREATE, READ, UPDATE, DELETE, SEARCH,
			HISTORY, PERMANENT_DELETE, WEBSOCKET);
	public static final Set<FhirServerRole> REMOTE_ORGANIZATION = EnumSet.of(CREATE, READ, UPDATE, DELETE, SEARCH,
			HISTORY);

	public static boolean isValid(String role)
	{
		return role != null && !role.isBlank() && Stream.of(values()).map(Enum::name).anyMatch(n -> n.equals(role));
	}
}
