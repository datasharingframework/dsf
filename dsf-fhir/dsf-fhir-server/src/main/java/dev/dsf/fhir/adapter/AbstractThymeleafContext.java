package dev.dsf.fhir.adapter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.DecimalType;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.IntegerType;
import org.hl7.fhir.r4.model.PrimitiveType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.UriType;
import org.hl7.fhir.r4.model.UrlType;

abstract class AbstractThymeleafContext<R extends Resource> implements ThymeleafContext
{
	private static final DateTimeFormatter DATE_DISPLAY_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
	private static final DateTimeFormatter DATE_TIME_DISPLAY_FORMAT = DateTimeFormatter
			.ofPattern("dd.MM.yyyy HH:mm:ss");

	private final Class<R> resourceType;
	private final String htmlFragment;

	protected AbstractThymeleafContext(Class<R> resourceType, String htmlFragment)
	{
		this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
		this.htmlFragment = Objects.requireNonNull(htmlFragment, "htmlFragment");
	}

	@Override
	public Class<R> getResourceType()
	{
		return resourceType;
	}

	@Override
	public String getHtmlFragment()
	{
		return htmlFragment;
	}

	@Override
	public final void setVariables(BiConsumer<String, Object> variables, Resource resource)
	{
		if (resourceType.isInstance(resource))
			doSetVariables(variables, resourceType.cast(resource));
		else
			throw new IllegalStateException("Unsupported resource of type " + resource.getClass().getName()
					+ ", expected " + resourceType.getName());
	}

	protected abstract void doSetVariables(BiConsumer<String, Object> variables, R resource);

	protected final String formatDate(Date date)
	{
		return format(date, DATE_DISPLAY_FORMAT);
	}

	protected final String formatDateTime(Date date)
	{
		return format(date, DATE_TIME_DISPLAY_FORMAT);
	}

	protected final String format(Date date, DateTimeFormatter formatter)
	{
		Objects.requireNonNull(formatter, "formatter");

		if (date == null)
			return null;
		else
			return formatter.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
	}

	protected final String formatLastUpdated(Resource resource)
	{
		return formatLastUpdated(resource, DATE_TIME_DISPLAY_FORMAT);
	}

	protected final String formatLastUpdated(Resource resource, DateTimeFormatter formatter)
	{
		Objects.requireNonNull(formatter, "formatter");

		if (resource == null || !resource.hasMeta() || !resource.getMeta().hasLastUpdated())
			return null;
		else
			return format(resource.getMeta().getLastUpdated(), formatter);
	}

	protected final <E> List<E> nullIfEmpty(List<E> list)
	{
		return list != null && list.isEmpty() ? null : list;
	}

	protected final <E extends Base, T> T getValue(E resource, Predicate<E> hasValue,
			Function<E, ? extends PrimitiveType<T>> getValue)
	{
		Objects.requireNonNull(hasValue, "hasValue");
		Objects.requireNonNull(getValue, "getValue");

		if (resource == null || !hasValue.test(resource))
			return null;

		PrimitiveType<T> type = getValue.apply(resource);
		return type.hasValue() ? type.getValue() : null;
	}

	protected final <E extends Base> String getString(E resource, Predicate<E> hasString,
			Function<E, StringType> getString)
	{
		return getValue(resource, hasString, getString);
	}

	protected final <E extends Base> String getDate(E resource, Predicate<E> hasDate, Function<E, DateType> getDate)
	{
		return formatDate(getValue(resource, hasDate, getDate));
	}

	protected final <E extends Base> String getDateTime(E resource, Predicate<E> hasDateTime,
			Function<E, DateTimeType> getDateTime)
	{
		return formatDateTime(getValue(resource, hasDateTime, getDateTime));
	}

	protected final <E extends Base> Boolean getBoolean(E resource, Predicate<E> hasBoolean,
			Function<E, BooleanType> getBoolean)
	{
		return getValue(resource, hasBoolean, getBoolean);
	}

	protected final <E extends Base> String getUri(E resource, Predicate<E> hasUri, Function<E, UriType> getUri)
	{
		return getValue(resource, hasUri, getUri);
	}

	protected final <E extends Base> String getUrl(E resource, Predicate<E> hasUrl, Function<E, UrlType> getUrl)
	{
		return getValue(resource, hasUrl, getUrl);
	}

	protected final <E extends Base> Integer getInteger(E resource, Predicate<E> hasInteger,
			Function<E, IntegerType> getInteger)
	{
		return getValue(resource, hasInteger, getInteger);
	}

	protected final <E extends Base> BigDecimal getDecimal(E resource, Predicate<E> hasDecimal,
			Function<E, DecimalType> getDecimal)
	{
		return getValue(resource, hasDecimal, getDecimal);
	}

	protected final <E extends Base> String getEnumeration(E resource, Predicate<E> hasEnumeration,
			Function<E, Enumeration<?>> getEnumeration)
	{
		Objects.requireNonNull(hasEnumeration, "hasEnumeration");
		Objects.requireNonNull(getEnumeration, "getEnumeration");

		if (resource == null || !hasEnumeration.test(resource))
			return null;

		Enumeration<?> e = getEnumeration.apply(resource);
		return e != null && e.hasCode() ? e.getCode() : null;
	}

	protected final <E extends Base> List<ElementSystemValue> getIdentifiers(E resource, Predicate<E> hasIdentifier,
			Function<E, List<Identifier>> getIdentifier)
	{
		Objects.requireNonNull(hasIdentifier, "hasIdentifier");
		Objects.requireNonNull(getIdentifier, "getIdentifier");

		if (resource == null || !hasIdentifier.test(resource))
			return null;

		List<Identifier> identifier = getIdentifier.apply(resource);
		return identifier != null ? identifier.stream().map(ElementSystemValue::from).toList() : null;
	}
}
