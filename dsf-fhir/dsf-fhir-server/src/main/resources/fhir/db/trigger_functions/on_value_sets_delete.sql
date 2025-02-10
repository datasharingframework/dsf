CREATE OR REPLACE FUNCTION on_value_sets_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.value_set_id);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL