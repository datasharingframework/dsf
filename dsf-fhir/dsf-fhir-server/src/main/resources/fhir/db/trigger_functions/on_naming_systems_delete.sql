CREATE OR REPLACE FUNCTION on_naming_systems_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.naming_system_id);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL