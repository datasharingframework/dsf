CREATE OR REPLACE FUNCTION tasks_unique() RETURNS TRIGGER AS $$
BEGIN
	IF NEW.task->>'status' = 'draft' THEN
		PERFORM pg_advisory_xact_lock(hashtext(jsonb_path_query_array(NEW.task, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/task-identifier").value')::text));
		IF EXISTS (SELECT 1 FROM current_tasks WHERE task_id <> NEW.task_id
			AND	((
					jsonb_path_exists(NEW.task, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/task-identifier").value')
					AND
					jsonb_path_query_array(task, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/task-identifier").value') @>
					jsonb_path_query_array(NEW.task, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/task-identifier").value')
				) OR (
					jsonb_path_exists(task, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/task-identifier").value')
					AND
					jsonb_path_query_array(NEW.task, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/task-identifier").value') @>
					jsonb_path_query_array(task, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/task-identifier").value')
				)
			)) THEN
			RAISE EXCEPTION 'Conflict: Not inserting Task with identifier.value %, resource already exists with given identifier.value',
				jsonb_path_query_array(NEW.task, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/task-identifier").value') USING ERRCODE = 'unique_violation';
		ELSE
			RETURN NEW;
		END IF;
	ELSE
		RETURN NEW;
	END IF;
END;
$$ LANGUAGE PLPGSQL