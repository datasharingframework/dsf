CREATE OR REPLACE FUNCTION on_code_systems_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.code_system_id);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL