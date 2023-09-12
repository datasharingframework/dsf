package dev.dsf.bpe.subscription;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Resource;

public interface PingEventResourceHandler<R extends Resource>
{
	void onPing(String ping, String subscriptionIdPart, Map<String, List<String>> searchCriteriaQueryParameters);
}