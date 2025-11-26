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
