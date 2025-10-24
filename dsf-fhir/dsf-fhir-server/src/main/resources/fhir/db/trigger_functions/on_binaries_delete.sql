CREATE OR REPLACE FUNCTION on_binaries_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.binary_id);

	IF (OLD.binary_oid IS NOT NULL) THEN
		INSERT INTO binaries_lo_unlink_queue (binary_oid) VALUES (OLD.binary_oid) ON CONFLICT DO NOTHING;
	END IF;

	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL