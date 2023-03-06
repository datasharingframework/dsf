package dev.dsf.fhir.subscription;

import dev.dsf.common.auth.Identity;
import jakarta.websocket.Session;

public interface WebSocketSubscriptionManager
{
	void bind(Identity identity, Session session, String subscriptionIdPart);

	void close(String sessionId);
}
