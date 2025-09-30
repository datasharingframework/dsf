CREATE OR REPLACE FUNCTION on_binaries_lo_unlink_queue_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM lo_unlink(OLD.binary_oid);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL