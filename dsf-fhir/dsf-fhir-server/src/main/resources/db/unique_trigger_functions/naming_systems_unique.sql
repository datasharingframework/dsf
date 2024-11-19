CREATE OR REPLACE FUNCTION naming_systems_unique() RETURNS TRIGGER AS $$
BEGIN
	PERFORM pg_advisory_xact_lock(hashtext(NEW.naming_system->>'name'));
	IF EXISTS (SELECT 1 FROM current_naming_systems WHERE naming_system_id <> NEW.naming_system_id
		AND (naming_system->>'name' = NEW.naming_system->>'name'
		OR (jsonb_path_exists(NEW.naming_system, '$.uniqueId[*] ? (@.type == "other").value')
			AND jsonb_path_query_array(naming_system, '$.uniqueId[*] ? (@.type == "other").value') @>
				jsonb_path_query_array(NEW.naming_system, '$.uniqueId[*] ? (@.type == "other").value')
		))) THEN
		RAISE EXCEPTION 'Conflict: Not inserting NamingSystem with name % and uniqueId.value %, resource already exists with given name or uniqueId.value',
			NEW.naming_system->>'name', jsonb_path_query_array(NEW.naming_system, '$.uniqueId[*] ? (@.type == "other").value') USING ERRCODE = 'unique_violation';
	ELSE
		RETURN NEW;
	END IF;
END;
$$ LANGUAGE PLPGSQL