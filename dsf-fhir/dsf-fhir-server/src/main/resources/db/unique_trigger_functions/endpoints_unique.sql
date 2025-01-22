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