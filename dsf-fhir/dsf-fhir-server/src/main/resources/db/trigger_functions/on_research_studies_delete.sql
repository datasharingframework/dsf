CREATE OR REPLACE FUNCTION on_research_studies_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.research_study_id);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL