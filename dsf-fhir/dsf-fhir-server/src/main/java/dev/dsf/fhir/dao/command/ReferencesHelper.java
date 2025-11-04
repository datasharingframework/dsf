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
import java.util.Map;
import java.util.function.Predicate;

import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.service.ResourceReference;
import jakarta.ws.rs.WebApplicationException;

public interface ReferencesHelper<R extends Resource>
{
	void resolveTemporaryAndConditionalReferencesOrLiteralInternalRelatedArtifactOrAttachmentUrls(
			Map<String, IdType> idTranslationTable, Connection connection) throws WebApplicationException;

	void resolveLogicalReferences(Connection connection) throws WebApplicationException;

	void checkReferences(Map<String, IdType> idTranslationTable, Connection connection,
			Predicate<ResourceReference> checkReference) throws WebApplicationException;
}