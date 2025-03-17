CREATE OR REPLACE FUNCTION on_resources_delete(deleted_resource_id uuid) RETURNS void AS $$
BEGIN
	DELETE FROM read_access WHERE resource_id = deleted_resource_id;
END;
$$ LANGUAGE PLPGSQL