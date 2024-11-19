CREATE OR REPLACE FUNCTION code_systems_unique() RETURNS TRIGGER AS $$
BEGIN
	PERFORM pg_advisory_xact_lock(hashtext((NEW.code_system->>'url') || (NEW.code_system->>'version')));
	IF EXISTS (SELECT 1 FROM current_code_systems WHERE code_system_id <> NEW.code_system_id
		AND code_system->>'url' = NEW.code_system->>'url'
		AND code_system->>'version' = NEW.code_system->>'version') THEN
		RAISE EXCEPTION 'Conflict: Not inserting CodeSystem with url % and version %, resource already exists with given url and version',
			NEW.code_system->>'url', NEW.code_system->>'version' USING ERRCODE = 'unique_violation';
	ELSE
		RETURN NEW;
	END IF;
END;
$$ LANGUAGE PLPGSQL