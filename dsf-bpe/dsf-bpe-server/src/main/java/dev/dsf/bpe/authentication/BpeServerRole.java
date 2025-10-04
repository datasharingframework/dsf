package dev.dsf.bpe.authentication;

import dev.dsf.common.auth.conf.DsfRole;
import dev.dsf.common.auth.conf.RoleConfig.RoleKeyAndValues;

public enum BpeServerRole implements DsfRole
{
	ADMIN;

	public static BpeServerRole from(RoleKeyAndValues role)
	{
		return role != null && role.key() != null && !role.key().isBlank() && ADMIN.name().equals(role.key())
				&& role.values().isEmpty() ? ADMIN : null;
	}

	@Override
	public boolean matches(DsfRole role)
	{
		return ADMIN.equals(role);
	}
}
