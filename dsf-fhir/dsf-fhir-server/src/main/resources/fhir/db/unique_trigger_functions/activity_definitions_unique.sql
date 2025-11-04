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

CREATE OR REPLACE FUNCTION activity_definitions_unique() RETURNS TRIGGER AS $$
BEGIN
	PERFORM pg_advisory_xact_lock(hashtext((NEW.activity_definition->>'url') || (NEW.activity_definition->>'version')));
	IF EXISTS (SELECT 1 FROM current_activity_definitions WHERE activity_definition_id <> NEW.activity_definition_id
		AND activity_definition->>'url' = NEW.activity_definition->>'url'
		AND activity_definition->>'version' = NEW.activity_definition->>'version') THEN
		RAISE EXCEPTION 'Conflict: Not inserting ActivityDefinition with url % and version %, resource already exists with given url and version',
			NEW.activity_definition->>'url', NEW.activity_definition->>'version' USING ERRCODE = 'unique_violation';
	ELSE
		RETURN NEW;
	END IF;
END;
$$ LANGUAGE PLPGSQL