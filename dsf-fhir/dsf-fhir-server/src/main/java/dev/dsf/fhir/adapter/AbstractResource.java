package dev.dsf.fhir.adapter;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Enumeration;
import org.hl7.fhir.r4.model.Resource;

import ca.uhn.fhir.model.api.annotation.ResourceDef;

abstract class AbstractResource<R extends Resource> extends AbstractResourceThymeleafContext<R>
{
	private record ResourceData(String type, String id, String version, String lastUpdated, String profiles,
			Boolean active, String status)
	{
	}

	static class ActiveOrStatus<R extends Resource>
	{
		static <R extends Resource> ActiveOrStatus<R> active(Predicate<R> hasActive, Function<R, BooleanType> getActive)
		{
			return new ActiveOrStatus<>(Objects.requireNonNull(hasActive, "hasActive"),
					Objects.requireNonNull(getActive, "getActive"), null, null);
		}

		static <R extends Resource> ActiveOrStatus<R> status(Predicate<R> hasStatus,
				Function<R, Enumeration<?>> getStatus)
		{
			return new ActiveOrStatus<>(null, null, Objects.requireNonNull(hasStatus, "hasStatus"),
					Objects.requireNonNull(getStatus, "getStatus"));
		}

		private final Predicate<R> hasActive;
		private final Function<R, BooleanType> getActive;
		private final Predicate<R> hasStatus;
		private final Function<R, Enumeration<?>> getStatus;

		ActiveOrStatus(Predicate<R> hasActive, Function<R, BooleanType> getActive, Predicate<R> hasStatus,
				Function<R, Enumeration<?>> getStatus)
		{
			this.hasActive = hasActive;
			this.getActive = getActive;
			this.hasStatus = hasStatus;
			this.getStatus = getStatus;
		}

		Boolean getActive(R resource)
		{
			return hasActive != null && getActive != null && hasActive.test(resource)
					&& getActive.apply(resource).hasValue() ? getActive.apply(resource).getValue() : null;
		}

		String getStatus(R resource)
		{
			return hasStatus != null && getStatus != null && hasStatus.test(resource)
					&& getStatus.apply(resource).hasCode() ? getStatus.apply(resource).getCode() : null;
		}
	}

	private final String htmlResourceFragment;
	private final ActiveOrStatus<R> activeOrStatus;

	protected AbstractResource(Class<R> resourceType, ActiveOrStatus<R> activeOrStatus)
	{
		this(resourceType, activeOrStatus, "resource" + resourceType.getAnnotation(ResourceDef.class).name());
	}

	protected AbstractResource(Class<R> resourceType, ActiveOrStatus<R> activeOrStatus, String htmlResourceFragment)
	{
		super(resourceType, "resource");

		this.activeOrStatus = activeOrStatus;
		this.htmlResourceFragment = htmlResourceFragment;
	}

	@Override
	public boolean isResourceSupported(String requestPathLastElement)
	{
		return true;
	}

	@Override
	protected final void doSetVariables(BiConsumer<String, Object> variables, R resource)
	{
		String type = getResourceType().getAnnotation(ResourceDef.class).name();
		String id = resource.hasIdElement() ? resource.getIdElement().getIdPart() : "";
		String version = resource.hasIdElement() ? resource.getIdElement().getVersionIdPart() : "";
		String lastUpdated = formatLastUpdated(resource);
		String profiles = resource.hasMeta() && resource.getMeta().hasProfile()
				? resource.getMeta().getProfile().stream().filter(CanonicalType::hasValue).map(CanonicalType::getValue)
						.map(s -> s.replace("|", " | ")).collect(Collectors.joining(", "))
				: null;

		Boolean active = activeOrStatus == null ? null : activeOrStatus.getActive(resource);
		String status = activeOrStatus == null ? null : activeOrStatus.getStatus(resource);

		variables.accept("htmlResourceFragment", htmlResourceFragment);
		variables.accept("resource", new ResourceData(type, id, version, lastUpdated, profiles, active, status));

		String resourceTypeName = getResourceType().getAnnotation(ResourceDef.class).name();
		variables.accept(resourceTypeName.substring(0, 1).toLowerCase() + resourceTypeName.substring(1),
				toElement(resource));

		doSetAdditionalVariables(variables, resource);
	}

	protected abstract Object toElement(R resource);

	protected void doSetAdditionalVariables(BiConsumer<String, Object> variables, R resource)
	{
	}
}
