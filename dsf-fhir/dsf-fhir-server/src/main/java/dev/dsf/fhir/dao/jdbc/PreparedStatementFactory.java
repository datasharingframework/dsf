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
package dev.dsf.fhir.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hl7.fhir.r4.model.Resource;
import org.postgresql.util.PGobject;

import ca.uhn.fhir.parser.IParser;

interface PreparedStatementFactory<R extends Resource>
{
	IParser getJsonParser();

	PGobject resourceToPgObject(R resource);

	PGobject uuidToPgObject(UUID uuid);

	String getCreateSql();

	void configureCreateStatement(LargeObjectManager largeObjectManager, PreparedStatement statement, R resource,
			UUID uuid) throws SQLException;

	String getReadByIdSql();

	void configureReadByIdStatement(PreparedStatement statement, UUID uuid) throws SQLException;

	LocalDateTime getReadByIdDeleted(ResultSet result) throws SQLException;

	long getReadByIdVersion(ResultSet result) throws SQLException;

	R getReadByIdResource(ResultSet result) throws SQLException;

	String getReadByIdAndVersionSql();

	void configureReadByIdAndVersionStatement(PreparedStatement statement, UUID uuid, long version) throws SQLException;

	LocalDateTime getReadByIdVersionDeleted(ResultSet result) throws SQLException;

	long getReadByIdVersionVersion(ResultSet result) throws SQLException;

	R getReadByIdAndVersionResource(ResultSet result) throws SQLException;

	String getUpdateSql();

	void configureUpdateSqlStatement(LargeObjectManager largeObjectManager, PreparedStatement statement, UUID uuid,
			long version, R resource) throws SQLException;
}
