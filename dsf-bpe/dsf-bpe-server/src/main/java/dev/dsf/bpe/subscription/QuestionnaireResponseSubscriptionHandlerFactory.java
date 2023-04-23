package dev.dsf.bpe.subscription;

import java.util.Objects;

import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.bpe.dao.LastEventTimeDao;
import dev.dsf.fhir.client.FhirWebserviceClient;

public class QuestionnaireResponseSubscriptionHandlerFactory
		implements SubscriptionHandlerFactory<QuestionnaireResponse>, InitializingBean
{
	private final ResourceHandler<QuestionnaireResponse> resourceHandler;
	private final LastEventTimeDao lastEventTimeDao;

	public QuestionnaireResponseSubscriptionHandlerFactory(ResourceHandler<QuestionnaireResponse> resourceHandler,
			LastEventTimeDao lastEventTimeDao)
	{
		this.resourceHandler = resourceHandler;
		this.lastEventTimeDao = lastEventTimeDao;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(resourceHandler, "resourceHandler");
		Objects.requireNonNull(lastEventTimeDao, "lastEventTimeDao");
	}

	@Override
	public ExistingResourceLoader<QuestionnaireResponse> createExistingResourceLoader(FhirWebserviceClient client)
	{
		return new ExistingResourceLoaderImpl<>(lastEventTimeDao, resourceHandler, client, "QuestionnaireResponse",
				QuestionnaireResponse.class);
	}

	@Override
	public EventResourceHandler<QuestionnaireResponse> createEventResourceHandler()
	{
		return new EventResourceHandlerImpl<>(lastEventTimeDao, resourceHandler, QuestionnaireResponse.class);
	}

	@Override
	public PingEventResourceHandler<QuestionnaireResponse> createPingEventResourceHandler(
			ExistingResourceLoader<QuestionnaireResponse> existingResourceLoader)
	{
		return new PingEventResourceHandler<>(existingResourceLoader);
	}
}
