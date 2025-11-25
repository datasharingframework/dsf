--
-- Copyright 2018-2025 Heilbronn University of Applied Sciences
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

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