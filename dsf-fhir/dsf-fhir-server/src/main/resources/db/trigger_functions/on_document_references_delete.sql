CREATE OR REPLACE FUNCTION on_document_references_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.document_reference_id);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL