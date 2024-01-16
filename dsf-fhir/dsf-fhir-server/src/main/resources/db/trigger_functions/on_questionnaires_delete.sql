CREATE OR REPLACE FUNCTION on_questionnaires_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.questionnaire_id);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL