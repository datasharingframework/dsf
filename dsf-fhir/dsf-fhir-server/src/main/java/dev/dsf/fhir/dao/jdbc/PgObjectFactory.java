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

import java.sql.SQLException;
import java.util.UUID;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Resource;
import org.postgresql.util.PGobject;

import ca.uhn.fhir.parser.IParser;

public interface PgObjectFactory
{
	interface JsonParameter
	{
	}

	IParser getJsonParser();

	PGobject resourceToPgObject(Resource resource) throws SQLException;

	PGobject jsonParameterToPgObject(JsonParameter parameter) throws SQLException;

	PGobject jsonParameterToPgObjectAsArray(JsonParameter... parameter) throws SQLException;

	PGobject uuidToPgObject(UUID uuid) throws SQLException;

	record ExtensionParameterValueString(String url, String valueString) implements JsonParameter
	{
		public static ExtensionParameterValueString thumbprint(String value)
		{
			return new ExtensionParameterValueString(
					"http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint", value);
		}
	}

	record IdentifierParameter(String system, String value) implements JsonParameter
	{
		public static IdentifierParameter organization(String value)
		{
			return new IdentifierParameter("http://dsf.dev/sid/organization-identifier", value);
		}

		public static IdentifierParameter endpoint(String value)
		{
			return new IdentifierParameter("http://dsf.dev/sid/endpoint-identifier", value);
		}
	}

	record CodingParameter(String system, String code) implements JsonParameter
	{
		public static CodingParameter coding(Coding coding)
		{
			return new CodingParameter(coding.getSystem(), coding.getCode());
		}
	}

	record ReferenceParameter(String reference) implements JsonParameter
	{
	}

	record RelatedArtifactParameter(String type, String resource) implements JsonParameter
	{
		public static RelatedArtifactParameter dependsOn(String resource)
		{
			return new RelatedArtifactParameter("depends-on", resource);
		}
	}
}
