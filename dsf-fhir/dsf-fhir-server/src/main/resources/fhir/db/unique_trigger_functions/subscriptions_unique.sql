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

CREATE OR REPLACE FUNCTION subscriptions_unique() RETURNS TRIGGER AS $$
BEGIN
	PERFORM pg_advisory_xact_lock(hashtext((NEW.subscription->>'criteria') || (NEW.subscription->'channel'->>'type')));
	IF EXISTS (SELECT 1 FROM current_subscriptions WHERE subscription_id <> NEW.subscription_id
		AND subscription->>'criteria' = NEW.subscription->>'criteria'
		AND subscription->'channel'->>'type' = NEW.subscription->'channel'->>'type'
		AND ((subscription->'channel'->>'payload' = NEW.subscription->'channel'->>'payload')
			OR (NOT subscription->'channel' ? 'payload' AND NOT NEW.subscription->'channel' ? 'payload'))) THEN
		RAISE EXCEPTION 'Conflict: Not inserting Subscription with criteria %, channel.type % and channel.payload %, resource already exists with given criteria, channel type and channel payload',
			NEW.subscription->>'criteria', NEW.subscription->'channel'->>'type', NEW.subscription->'channel'->>'payload' USING ERRCODE = 'unique_violation';
	ELSE
		RETURN NEW;
	END IF;
END;
$$ LANGUAGE PLPGSQL