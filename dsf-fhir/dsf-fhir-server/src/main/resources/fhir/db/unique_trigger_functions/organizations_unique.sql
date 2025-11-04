--
-- Copyright 2018-2025 Heilbronn University of Applied Sciences
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE OR REPLACE FUNCTION organizations_unique() RETURNS TRIGGER AS $$
BEGIN
	PERFORM pg_advisory_xact_lock(hashtext(jsonb_path_query_array(NEW.organization, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/organization-identifier").value')::text));
	IF jsonb_path_exists(NEW.organization, '$.meta.profile[*] ? (@ == "http://dsf.dev/fhir/StructureDefinition/organization")')
		AND EXISTS (SELECT 1 FROM current_organizations WHERE organization_id <> NEW.organization_id
		AND ((
				jsonb_path_exists(NEW.organization, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/organization-identifier").value')
				AND
				jsonb_path_query_array(organization, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/organization-identifier").value') @>
				jsonb_path_query_array(NEW.organization, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/organization-identifier").value')
			) OR (
				jsonb_path_exists(organization, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/organization-identifier").value')
				AND
				jsonb_path_query_array(NEW.organization, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/organization-identifier").value') @>
				jsonb_path_query_array(organization, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/organization-identifier").value')
			) OR (
				jsonb_path_exists(NEW.organization, '$.extension[*] ? (@.url == "http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint").valueString')
				AND
				jsonb_path_query_array(organization, '$.extension[*] ? (@.url == "http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint").valueString') @>
				jsonb_path_query_array(NEW.organization, '$.extension[*] ? (@.url == "http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint").valueString')
			) OR (
				jsonb_path_exists(organization, '$.extension[*] ? (@.url == "http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint").valueString')
				AND
				jsonb_path_query_array(NEW.organization, '$.extension[*] ? (@.url == "http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint").valueString') @>
				jsonb_path_query_array(organization, '$.extension[*] ? (@.url == "http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint").valueString')
			)
		)) THEN
		RAISE EXCEPTION 'Conflict: Not inserting member Organization with thumbprint % and identifier.value %, resource already exists with given thumbprint or identifier.value',
			jsonb_path_query_array(NEW.organization, '$.extension[*] ? (@.url == "http://dsf.dev/fhir/StructureDefinition/extension-certificate-thumbprint").valueString'),
			jsonb_path_query_array(NEW.organization, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/organization-identifier").value') USING ERRCODE = 'unique_violation';
	
	ELSIF jsonb_path_exists(NEW.organization, '$.meta.profile[*] ? (@ == "http://dsf.dev/fhir/StructureDefinition/organization-parent")')
		AND EXISTS (SELECT 1 FROM current_organizations WHERE organization_id <> NEW.organization_id
		AND ((
				jsonb_path_exists(NEW.organization, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/organization-identifier").value')
				AND
				jsonb_path_query_array(organization, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/organization-identifier").value') @>
				jsonb_path_query_array(NEW.organization, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/organization-identifier").value')
			) OR (
				jsonb_path_exists(organization, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/organization-identifier").value')
				AND
				jsonb_path_query_array(NEW.organization, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/organization-identifier").value') @>
				jsonb_path_query_array(organization, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/organization-identifier").value')
			)
		)) THEN
		RAISE EXCEPTION 'Conflict: Not inserting parent Organization with identifier.value %, resource already exists with identifier.value',
			jsonb_path_query_array(NEW.organization, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/organization-identifier").value') USING ERRCODE = 'unique_violation';
	ELSE
		RETURN NEW;
	END IF;
END;
$$ LANGUAGE PLPGSQL