CREATE OR REPLACE FUNCTION subscriptions_unique() RETURNS TRIGGER AS $$
BEGIN
	PERFORM pg_advisory_xact_lock(hashtext((NEW.subscription->>'criteria') || (NEW.subscription->'channel'->>'type') || (NEW.subscription->'channel'->>'payload')));
	IF EXISTS (SELECT 1 FROM current_subscriptions WHERE subscription_id <> NEW.subscription_id
		AND subscription->>'criteria' = NEW.subscription->>'criteria'
		AND subscription->'channel'->>'type' = NEW.subscription->'channel'->>'type'
		AND subscription->'channel'->>'payload' = NEW.subscription->'channel'->>'payload') THEN
		RAISE EXCEPTION 'Conflict: Not inserting Subscription with criteria %, channel.type % and channel.payload %, resource already exists with given criteria, channel type and channel payload',
			NEW.subscription->>'criteria', NEW.subscription->'channel'->>'type', NEW.subscription->'channel'->>'payload' USING ERRCODE = 'unique_violation';
	ELSE
		RETURN NEW;
	END IF;
END;
$$ LANGUAGE PLPGSQL