CREATE OR REPLACE FUNCTION on_binaries_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.binary_id);
	PERFORM lo_unlink(OLD.binary_oid);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL