CREATE OR REPLACE FUNCTION on_practitioner_roles_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.practitioner_role_id);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL