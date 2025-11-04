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

CREATE OR REPLACE FUNCTION on_practitioner_roles_insert() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_insert(NEW.practitioner_role_id, NEW.version, NEW.practitioner_role);
	RETURN NEW;
END;
$$ LANGUAGE PLPGSQL