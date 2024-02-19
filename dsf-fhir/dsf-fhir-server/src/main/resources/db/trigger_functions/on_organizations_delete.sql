CREATE OR REPLACE FUNCTION on_organizations_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.organization_id);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL