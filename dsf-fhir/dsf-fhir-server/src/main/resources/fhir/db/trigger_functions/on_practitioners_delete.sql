CREATE OR REPLACE FUNCTION on_practitioners_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.practitioner_id);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL