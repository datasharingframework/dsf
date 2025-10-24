package dev.dsf.bpe.subscription;

import java.sql.SQLException;
import java.util.Date;

import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.bpe.dao.LastEventTimeDao;

public class EventResourceHandlerImpl<R extends Resource> implements EventResourceHandler<R>
{
	private static final Logger logger = LoggerFactory.getLogger(EventResourceHandlerImpl.class);

	private final LastEventTimeDao lastEventTimeDao;
	private final ResourceHandler<R> handler;
	private final Class<R> resourceClass;

	public EventResourceHandlerImpl(LastEventTimeDao lastEventTimeDao, ResourceHandler<R> handler,
			Class<R> resourceClass)
	{
		this.lastEventTimeDao = lastEventTimeDao;
		this.handler = handler;
		this.resourceClass = resourceClass;
	}

	@Override
	public void onResource(Resource resource)
	{
		logger.trace("Resource of type {} received", resource.getClass().getAnnotation(ResourceDef.class).name());

		if (resourceClass.isInstance(resource))
		{
			handler.onResource(resourceClass.cast(resource));
			writeLastEventTime(resource.getMeta().getLastUpdated());
		}
		else
		{
			logger.warn("Ignoring resource of type {}", resource.getClass().getAnnotation(ResourceDef.class).name());
		}
	}

	private void writeLastEventTime(Date lastUpdated)
	{
		try
		{
			lastEventTimeDao.writeLastEventTime(lastUpdated);
		}
		catch (SQLException e)
		{
			logger.debug("Unable to write last event time to db", e);
			logger.warn("Unable to write last event time to db: {} - {}", e.getClass().getName(), e.getMessage());

			throw new RuntimeException(e);
		}
	}
}
