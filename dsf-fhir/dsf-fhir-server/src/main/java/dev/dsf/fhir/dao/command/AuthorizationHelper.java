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
package dev.dsf.fhir.dao.command;

import java.sql.Connection;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.common.auth.conf.Identity;
import jakarta.ws.rs.WebApplicationException;

public interface AuthorizationHelper
{
	void checkCreateAllowed(int index, Connection connection, Identity identity, Resource newResource)
			throws WebApplicationException;

	void checkReadAllowed(int index, Connection connection, Identity identity, Resource existingResource)
			throws WebApplicationException;

	void checkUpdateAllowed(int index, Connection connection, Identity identity, Resource oldResource,
			Resource newResource) throws WebApplicationException;

	void checkDeleteAllowed(int index, Connection connection, Identity identity, Resource oldResource)
			throws WebApplicationException;

	void checkSearchAllowed(int index, Identity identity, String resourceTypeName) throws WebApplicationException;

	void filterIncludeResults(int index, Connection connection, Identity identity, Bundle multipleResult);
}
