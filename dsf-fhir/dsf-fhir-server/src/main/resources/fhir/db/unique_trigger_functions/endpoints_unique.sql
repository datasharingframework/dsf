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

CREATE OR REPLACE FUNCTION endpoints_unique() RETURNS TRIGGER AS $$
BEGIN
	PERFORM pg_advisory_xact_lock(hashtext(NEW.endpoint->>'address'));
	IF EXISTS (SELECT 1 FROM current_endpoints WHERE endpoint_id <> NEW.endpoint_id
		AND (
			endpoint->>'address' = NEW.endpoint->>'address'
			OR (
				jsonb_path_exists(NEW.endpoint, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/endpoint-identifier").value')
				AND
				jsonb_path_query_array(endpoint, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/endpoint-identifier").value') @>
				jsonb_path_query_array(NEW.endpoint, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/endpoint-identifier").value')
			) OR (
				jsonb_path_exists(endpoint, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/endpoint-identifier").value')
				AND
				jsonb_path_query_array(NEW.endpoint, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/endpoint-identifier").value') @>
				jsonb_path_query_array(endpoint, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/endpoint-identifier").value')
			)
		)) THEN
		RAISE EXCEPTION 'Conflict: Not inserting Endpoint with address % and identifier.value %, resource already exists with given address or identifier.value',
			NEW.endpoint->>'address', jsonb_path_query_array(NEW.endpoint, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/endpoint-identifier").value') USING ERRCODE = 'unique_violation';
	ELSE
		RETURN NEW;
	END IF;
END;
$$ LANGUAGE PLPGSQL