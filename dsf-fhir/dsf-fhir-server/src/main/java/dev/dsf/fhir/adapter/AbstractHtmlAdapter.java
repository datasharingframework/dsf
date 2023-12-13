package dev.dsf.fhir.adapter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

import org.hl7.fhir.r4.model.Resource;

public abstract class AbstractHtmlAdapter
{
	protected static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	protected static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
	protected static final DateTimeFormatter DATE_TIME_DISPLAY_FORMAT = DateTimeFormatter
			.ofPattern("dd.MM.yyyy HH:mm:ss");

	/**
	 * @param date
	 *            may be <code>null</code>
	 * @param formatter
	 *            not <code>null</code>
	 * @return empty String if given date is <code>null</code>
	 */
	protected String format(Date date, DateTimeFormatter formatter)
	{
		Objects.requireNonNull(formatter, "formatter");

		if (date == null)
			return "";
		else
			return formatter.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
	}

	/**
	 * @param resource
	 *            may be <code>null</code>
	 * @param formatter
	 *            not <code>null</code>
	 * @return empty String if given resource is <code>null</code> or has no meta or meta.lastUpdated
	 */
	protected String formatLastUpdated(Resource resource, DateTimeFormatter formatter)
	{
		Objects.requireNonNull(formatter, "formatter");

		if (resource == null || !resource.hasMeta() || !resource.getMeta().hasLastUpdated())
			return "";
		else
			return format(resource.getMeta().getLastUpdated(), formatter);
	}
}
