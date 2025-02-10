CREATE OR REPLACE FUNCTION activity_definitions_unique() RETURNS TRIGGER AS $$
BEGIN
	PERFORM pg_advisory_xact_lock(hashtext((NEW.activity_definition->>'url') || (NEW.activity_definition->>'version')));
	IF EXISTS (SELECT 1 FROM current_activity_definitions WHERE activity_definition_id <> NEW.activity_definition_id
		AND activity_definition->>'url' = NEW.activity_definition->>'url'
		AND activity_definition->>'version' = NEW.activity_definition->>'version') THEN
		RAISE EXCEPTION 'Conflict: Not inserting ActivityDefinition with url % and version %, resource already exists with given url and version',
			NEW.activity_definition->>'url', NEW.activity_definition->>'version' USING ERRCODE = 'unique_violation';
	ELSE
		RETURN NEW;
	END IF;
END;
$$ LANGUAGE PLPGSQL