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
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;

import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.IdType;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.dao.jdbc.LargeObjectManager;
import dev.dsf.fhir.event.EventHandler;
import dev.dsf.fhir.validation.SnapshotGenerator;
import jakarta.ws.rs.WebApplicationException;

public interface Command
{
	String URL_UUID_PREFIX = "urn:uuid:";

	int getIndex();

	int getTransactionPriority();

	default void preExecute(Map<String, IdType> idTranslationTable, Connection connection,
			ValidationHelper validationHelper, SnapshotGenerator snapshotGenerator)
	{
	}

	void execute(Map<String, IdType> idTranslationTable, LargeObjectManager largeObjectManager, Connection connection,
			ValidationHelper validationHelper) throws SQLException, WebApplicationException;

	default Optional<BundleEntryComponent> postExecute(Connection connection, EventHandler eventHandler)
	{
		return Optional.empty();
	}

	Identity getIdentity();

	String getResourceTypeName();
}
