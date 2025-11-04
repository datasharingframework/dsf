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

CREATE OR REPLACE FUNCTION on_binaries_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.binary_id);

	IF (OLD.binary_oid IS NOT NULL) THEN
		INSERT INTO binaries_lo_unlink_queue (binary_oid) VALUES (OLD.binary_oid) ON CONFLICT DO NOTHING;
	END IF;

	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL