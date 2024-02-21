CREATE OR REPLACE FUNCTION on_subscriptions_delete() RETURNS TRIGGER AS $$
BEGIN
	PERFORM on_resources_delete(OLD.subscription_id);
	RETURN OLD;
END;
$$ LANGUAGE PLPGSQL