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

import java.util.Optional;
import java.util.Set;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Practitioner;

import dev.dsf.common.auth.DsfOpenIdCredentials;

public interface PractitionerIdentity extends Identity
{
	String PRACTITIONER_IDENTIFIER_SYSTEM = "http://dsf.dev/sid/practitioner-identifier";

	/**
	 * @return never <code>null</code>
	 */
	Practitioner getPractitioner();

	/**
	 * @return never <code>null</code>
	 */
	Set<Coding> getPractionerRoles();

	/**
	 * @return {@link Optional#empty()} if login via client certificate
	 */
	Optional<DsfOpenIdCredentials> getCredentials();
}
