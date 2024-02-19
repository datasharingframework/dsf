CREATE OR REPLACE FUNCTION on_healthcare_services_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.healthcare_service_id);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL