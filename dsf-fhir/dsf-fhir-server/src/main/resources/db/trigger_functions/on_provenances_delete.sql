CREATE OR REPLACE FUNCTION on_provenances_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.provenance_id);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL