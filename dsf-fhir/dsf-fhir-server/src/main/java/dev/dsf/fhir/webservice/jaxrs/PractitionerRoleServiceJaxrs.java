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
package dev.dsf.fhir.webservice.jaxrs;

import org.hl7.fhir.r4.model.PractitionerRole;

import dev.dsf.fhir.webservice.specification.PractitionerRoleService;
import jakarta.ws.rs.Path;

@Path(PractitionerRoleServiceJaxrs.PATH)
public class PractitionerRoleServiceJaxrs extends
		AbstractResourceServiceJaxrs<PractitionerRole, PractitionerRoleService> implements PractitionerRoleService
{
	public static final String PATH = "PractitionerRole";

	public PractitionerRoleServiceJaxrs(PractitionerRoleService delegate)
	{
		super(delegate);
	}
}
