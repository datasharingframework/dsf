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
package dev.dsf.fhir.webservice.specification;

import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StructureDefinition;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public interface StructureDefinitionService extends BasicResourceService<StructureDefinition>
{
	Response postSnapshotNew(String snapshotPath, Parameters parameters, UriInfo uri, HttpHeaders headers);

	Response getSnapshotNew(String snapshotPath, UriInfo uri, HttpHeaders headers);

	Response postSnapshotExisting(String snapshotPath, String id, UriInfo uri, HttpHeaders headers);

	Response getSnapshotExisting(String snapshotPath, String id, UriInfo uri, HttpHeaders headers);
}
