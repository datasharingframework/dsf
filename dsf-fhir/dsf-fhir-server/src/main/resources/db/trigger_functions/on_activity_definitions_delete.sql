CREATE OR REPLACE FUNCTION on_activity_definitions_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.activity_definition_id);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL