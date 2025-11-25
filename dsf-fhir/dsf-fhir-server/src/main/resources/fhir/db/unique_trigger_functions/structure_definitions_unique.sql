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

CREATE OR REPLACE FUNCTION structure_definitions_unique() RETURNS TRIGGER AS $$
BEGIN
	PERFORM pg_advisory_xact_lock(hashtext((NEW.structure_definition->>'url') || (NEW.structure_definition->>'version')));
	IF EXISTS (SELECT 1 FROM current_structure_definitions WHERE structure_definition_id <> NEW.structure_definition_id
		AND structure_definition->>'url' = NEW.structure_definition->>'url'
		AND structure_definition->>'version' = NEW.structure_definition->>'version') THEN
		RAISE EXCEPTION 'Conflict: Not inserting StructureDefinition with url % and version %, resource already exists with given url and version',
			NEW.structure_definition->>'url', NEW.structure_definition->>'version' USING ERRCODE = 'unique_violation';
	ELSE
		RETURN NEW;
	END IF;
END;
$$ LANGUAGE PLPGSQL