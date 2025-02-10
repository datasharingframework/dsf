CREATE OR REPLACE FUNCTION on_measure_reports_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.measure_report_id);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL