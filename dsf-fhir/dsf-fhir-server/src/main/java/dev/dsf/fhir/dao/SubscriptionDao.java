package dev.dsf.fhir.dao;

import java.sql.SQLException;
import java.util.List;

import org.hl7.fhir.r4.model.Subscription;
import org.hl7.fhir.r4.model.Subscription.SubscriptionStatus;

public interface SubscriptionDao extends ResourceDao<Subscription>
{
	List<Subscription> readByStatus(SubscriptionStatus status) throws SQLException;

	boolean existsByCriteriaChannelTypeAndChannelPayload(String criteria, String channelType, String channelPayload)
			throws SQLException;
}
