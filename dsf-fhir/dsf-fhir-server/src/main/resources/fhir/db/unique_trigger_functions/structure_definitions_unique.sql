CREATE OR REPLACE FUNCTION structure_definitions_unique() RETURNS TRIGGER AS $$
BEGIN
	PERFORM pg_advisory_xact_lock(hashtext((NEW.structure_definition->>'url') || (NEW.structure_definition->>'version')));
	IF EXISTS (SELECT 1 FROM current_structure_definitions WHERE structure_definition_id <> NEW.structure_definition_id
		AND structure_definition->>'url' = NEW.structure_definition->>'url'
		AND structure_definition->>'version' = NEW.structure_definition->>'version') THEN
		RAISE EXCEPTION 'Conflict: Not inserting StructureDefinition with url % and version %, resource already exists with given url and version',
			NEW.structure_definition->>'url', NEW.structure_definition->>'version' USING ERRCODE = 'unique_violation';
	ELSE
		RETURN NEW;
	END IF;
END;
$$ LANGUAGE PLPGSQL