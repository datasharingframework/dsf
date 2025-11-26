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

CREATE OR REPLACE FUNCTION organization_affiliations_unique() RETURNS TRIGGER AS $$
BEGIN
	PERFORM pg_advisory_xact_lock(hashtext((NEW.organization_affiliation->'organization'->>'reference') || (NEW.organization_affiliation->'participatingOrganization'->>'reference')));
	IF EXISTS (SELECT 1 FROM current_organization_affiliations WHERE organization_affiliation_id <> NEW.organization_affiliation_id
		AND organization_affiliation->'organization'->>'reference' = NEW.organization_affiliation->'organization'->>'reference'
		AND organization_affiliation->'participatingOrganization'->>'reference' = NEW.organization_affiliation->'participatingOrganization'->>'reference'
		AND ((
				jsonb_path_exists(NEW.organization_affiliation, '$.endpoint[*].reference')
				AND
				jsonb_path_query_array(organization_affiliation, '$.endpoint[*].reference') @>
				jsonb_path_query_array(NEW.organization_affiliation, '$.endpoint[*].reference')
			) OR (
				jsonb_path_exists(organization_affiliation, '$.endpoint[*].reference')
				AND
				jsonb_path_query_array(NEW.organization_affiliation, '$.endpoint[*].reference') @>
				jsonb_path_query_array(organization_affiliation, '$.endpoint[*].reference')
			) OR (
				jsonb_path_exists(NEW.organization_affiliation, '$.code[*].coding[*] ? (@.system == "http://dsf.dev/fhir/CodeSystem/organization-role").code')
				AND
				jsonb_path_query_array(organization_affiliation, '$.code[*].coding[*] ? (@.system == "http://dsf.dev/fhir/CodeSystem/organization-role").code') @>
				jsonb_path_query_array(NEW.organization_affiliation, '$.code[*].coding[*] ? (@.system == "http://dsf.dev/fhir/CodeSystem/organization-role").code')
			) OR (
				jsonb_path_exists(organization_affiliation, '$.code[*].coding[*] ? (@.system == "http://dsf.dev/fhir/CodeSystem/organization-role").code')
				AND
				jsonb_path_query_array(NEW.organization_affiliation, '$.code[*].coding[*] ? (@.system == "http://dsf.dev/fhir/CodeSystem/organization-role").code') @>
				jsonb_path_query_array(organization_affiliation, '$.code[*].coding[*] ? (@.system == "http://dsf.dev/fhir/CodeSystem/organization-role").code')
			)
		)) THEN
		RAISE EXCEPTION 'Conflict: Not inserting OrganizationAffiliation with parent organization %, member organization %, endpoint % and roles %, resource already exists with parent organization, member organization and endpoint or roles',
			NEW.organization_affiliation->'organization'->>'reference',
			NEW.organization_affiliation->'participatingOrganization'->>'reference',
			jsonb_path_query_array(NEW.organization_affiliation, '$.endpoint[*].reference'),
			jsonb_path_query_array(NEW.organization_affiliation, '$.code[*].coding[*] ? (@.system == "http://dsf.dev/fhir/CodeSystem/organization-role").code') USING ERRCODE = 'unique_violation';
	ELSE
		RETURN NEW;
	END IF;
END;
$$ LANGUAGE PLPGSQL