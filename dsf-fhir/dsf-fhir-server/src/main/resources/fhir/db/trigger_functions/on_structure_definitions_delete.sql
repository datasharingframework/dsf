CREATE OR REPLACE FUNCTION on_structure_definitions_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.structure_definition_id);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL