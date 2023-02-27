package dev.dsf.fhir.subscription;

import javax.websocket.Session;

import dev.dsf.fhir.authentication.User;

public interface WebSocketSubscriptionManager
{
	void bind(User user, Session session, String subscriptionIdPart);

	void close(String sessionId);
}
