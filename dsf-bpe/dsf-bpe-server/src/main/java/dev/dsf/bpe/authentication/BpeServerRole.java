package dev.dsf.bpe.authentication;

import java.util.stream.Stream;

import dev.dsf.common.auth.conf.DsfRole;

public enum BpeServerRole implements DsfRole
{
	ADMIN;

	public static boolean isValid(String role)
	{
		return role != null && !role.isBlank() && Stream.of(values()).map(Enum::name).anyMatch(n -> n.equals(role));
	}
}
