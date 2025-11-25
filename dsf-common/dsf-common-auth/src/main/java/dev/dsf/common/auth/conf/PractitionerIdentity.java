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
package dev.dsf.common.auth.conf;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Practitioner;

import dev.dsf.common.auth.DsfOpenIdCredentials;

public interface PractitionerIdentity extends Identity
{
	String CODE_SYSTEM_PRACTITIONER_ROLE = "http://dsf.dev/fhir/CodeSystem/practitioner-role";
	String PRACTITIONER_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/practitioner-identifier";

	/**
	 * @return never <code>null</code>
	 */
	Practitioner getPractitioner();

	Optional<String> getPractitionerIdentifierValue();

	/**
	 * @return never <code>null</code>
	 */
	Set<Coding> getPractionerRoles();

	default boolean hasPractionerRole(String dsfRole)
	{
		return dsfRole != null && hasPractionerRole(new Coding(CODE_SYSTEM_PRACTITIONER_ROLE, dsfRole, null));
	}

	default boolean hasPractionerRole(Coding coding)
	{
		return coding != null && coding.hasSystem() && coding.hasCode()
				&& getPractionerRoles().stream().filter(Objects::nonNull).anyMatch(
						c -> coding.getSystem().equals(c.getSystem()) && coding.getCode().equals(c.getCode()));
	}

	/**
	 * @return {@link Optional#empty()} if login via client certificate
	 */
	Optional<DsfOpenIdCredentials> getCredentials();
}
