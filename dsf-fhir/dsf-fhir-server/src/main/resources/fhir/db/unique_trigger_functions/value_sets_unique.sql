CREATE OR REPLACE FUNCTION value_sets_unique() RETURNS TRIGGER AS $$
BEGIN
	PERFORM pg_advisory_xact_lock(hashtext((NEW.value_set->>'url') || (NEW.value_set->>'version')));
	IF EXISTS (SELECT 1 FROM current_value_sets WHERE value_set_id <> NEW.value_set_id
		AND value_set->>'url' = NEW.value_set->>'url'
		AND value_set->>'version' = NEW.value_set->>'version') THEN
		RAISE EXCEPTION 'Conflict: Not inserting ValueSet with url % and version %, resource already exists with given url and version',
			NEW.value_set->>'url', NEW.value_set->>'version' USING ERRCODE = 'unique_violation';
	ELSE
		RETURN NEW;
	END IF;
END;
$$ LANGUAGE PLPGSQL