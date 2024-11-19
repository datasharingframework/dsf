CREATE OR REPLACE FUNCTION organization_affiliations_unique() RETURNS TRIGGER AS $$
BEGIN
	PERFORM pg_advisory_xact_lock(hashtext((NEW.organization_affiliation->'organization'->>'reference') || (NEW.organization_affiliation->'participatingOrganization'->>'reference')));
	IF EXISTS (SELECT 1 FROM current_organization_affiliations WHERE organization_affiliation_id <> NEW.organization_affiliation_id
		AND organization_affiliation->'organization'->>'reference' = NEW.organization_affiliation->'organization'->>'reference'
		AND organization_affiliation->'participatingOrganization'->>'reference' = NEW.organization_affiliation->'participatingOrganization'->>'reference'
		AND ((	
			jsonb_path_exists(NEW.organization_affiliation, '$.endpoint[*].reference')
			AND jsonb_path_query_array(organization_affiliation, '$.endpoint[*].reference') @>
				jsonb_path_query_array(NEW.organization_affiliation, '$.endpoint[*].reference')
		) OR (
			jsonb_path_exists(NEW.organization_affiliation, '$.code[*].coding[*] ? (@.system == "http://dsf.dev/fhir/CodeSystem/organization-role").code')
			AND jsonb_path_query_array(organization_affiliation, '$.code[*].coding[*] ? (@.system == "http://dsf.dev/fhir/CodeSystem/organization-role").code') @>
				jsonb_path_query_array(NEW.organization_affiliation, '$.code[*].coding[*] ? (@.system == "http://dsf.dev/fhir/CodeSystem/organization-role").code')
		))) THEN
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