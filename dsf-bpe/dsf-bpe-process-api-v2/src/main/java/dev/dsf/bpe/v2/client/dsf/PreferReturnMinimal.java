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
package dev.dsf.bpe.v2.client.dsf;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Resource;

import jakarta.ws.rs.core.MediaType;

public interface PreferReturnMinimal
{
	IdType create(Resource resource);

	IdType createConditionaly(Resource resource, String ifNoneExistCriteria);

	IdType createBinary(InputStream in, MediaType mediaType, String securityContextReference);

	IdType update(Resource resource);

	IdType updateConditionaly(Resource resource, Map<String, List<String>> criteria);

	IdType updateBinary(String id, InputStream in, MediaType mediaType, String securityContextReference);

	Bundle postBundle(Bundle bundle);

	IdType operation(String operationName, Parameters parameters);

	<T extends Resource> IdType operation(Class<T> resourceType, String operationName, Parameters parameters);

	<T extends Resource> IdType operation(Class<T> resourceType, String id, String operationName,
			Parameters parameters);

	<T extends Resource> IdType operation(Class<T> resourceType, String id, String version, String operationName,
			Parameters parameters);
}