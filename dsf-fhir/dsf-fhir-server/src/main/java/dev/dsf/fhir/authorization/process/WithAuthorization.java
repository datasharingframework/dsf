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
package dev.dsf.fhir.authorization.process;

import org.hl7.fhir.r4.model.Coding;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.common.auth.conf.PractitionerIdentity;

public interface WithAuthorization
{
	Coding getProcessAuthorizationCode();

	boolean matches(Coding processAuthorizationCode);

	String getPractitionerRoleSystem();

	String getPractitionerRoleCode();

	default boolean needsPractitionerRole()
	{
		return getPractitionerRoleSystem() != null && getPractitionerRoleCode() != null;
	}

	default boolean hasPractitionerRole(Identity identity)
	{
		return identity instanceof PractitionerIdentity p
				&& p.getPractionerRoles().stream().anyMatch(c -> getPractitionerRoleSystem().equals(c.getSystem())
						&& getPractitionerRoleCode().equals(c.getCode()));
	}


	default boolean practitionerRoleMatches(Coding coding)
	{
		return coding != null && coding.hasSystem() && coding.hasCode()
				&& getPractitionerRoleSystem().equals(coding.getSystem())
				&& getPractitionerRoleCode().equals(coding.getCode());
	}
}
