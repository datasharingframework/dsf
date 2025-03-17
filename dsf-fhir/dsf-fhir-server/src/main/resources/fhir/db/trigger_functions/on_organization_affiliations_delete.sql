CREATE OR REPLACE FUNCTION on_organization_affiliations_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.organization_affiliation_id);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL