package dev.dsf.fhir.adapter;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

public final class ElementId
{
	/**
	 * @param <R>
	 *            FHIR resource type
	 * @param resource
	 *            may be <code>null</code>
	 * @param hasReferences
	 *            not <code>null</code>
	 * @param getReferences
	 *            not <code>null</code>
	 * @return <code>null</code> if the given <b>resource</b> is <code>null</code>, <b>hasReferences</b> returns
	 *         <code>false</code>, the List provided by <b>getReferences</b> is empty; references without
	 *         reference-element will be filtered from the returned list
	 */
	public static <R extends Resource> List<ElementId> fromList(R resource, Predicate<R> hasReferences,
			Function<R, List<Reference>> getReferences)
	{
		List<Reference> references = resource != null && hasReferences.test(resource) ? getReferences.apply(resource)
				: null;

		return references != null && !references.isEmpty() ? references.stream().filter(Reference::hasReferenceElement)
				.map(Reference::getReferenceElement).map(ElementId::from).toList() : null;
	}

	/**
	 * @param <R>
	 *            FHIR resource type
	 * @param resource
	 *            may be <code>null</code>
	 * @param hasReference
	 *            not <code>null</code>
	 * @param getReference
	 *            not <code>null</code>
	 * @return <code>null</code> if the given <b>resource</b> is <code>null</code>, <b>hasReference</b> returns
	 *         <code>false</code> or the {@link Reference} provided by <b>getReference</b> has not reference-element
	 */
	public static <R extends Resource> ElementId from(R resource, Predicate<R> hasReference,
			Function<R, Reference> getReference)
	{
		return from(resource, hasReference, getReference, false);
	}

	/**
	 * @param <R>
	 *            FHIR resource type
	 * @param resource
	 *            may be <code>null</code>
	 * @param hasReference
	 *            not <code>null</code>
	 * @param getReference
	 *            not <code>null</code>
	 * @param fullId
	 *            <code>true</code> to display id with resource-type and version
	 * @return <code>null</code> if the given <b>resource</b> is <code>null</code>, <b>hasReference</b> returns
	 *         <code>false</code> or the {@link Reference} provided by <b>getReference</b> has not reference-element
	 */
	public static <R extends Resource> ElementId from(R resource, Predicate<R> hasReference,
			Function<R, Reference> getReference, boolean fullId)
	{
		Reference ref = resource != null && hasReference.test(resource) ? getReference.apply(resource) : null;
		IIdType id = ref != null && ref.hasReferenceElement() ? ref.getReferenceElement() : null;

		return id != null ? ElementId.from(id, fullId) : null;
	}

	/**
	 * @param resource
	 *            may be <code>null</code>
	 * @return <code>null</code> if the given <b>resource</b> is <code>null</code> or has no id-element
	 */
	public static ElementId from(Resource resource)
	{
		return resource != null && resource.hasIdElement() ? ElementId.from(resource.getIdElement()) : null;
	}

	/**
	 * @param id
	 *            not <code>null</code>
	 * @return
	 * @throws NullPointerException
	 *             if the given <b>id</b> is <code>null</code>
	 */
	public static ElementId from(IIdType id)
	{
		return from(id, false);
	}

	/**
	 * @param id
	 *            not <code>null</code>
	 * @param fullId
	 *            <code>true</code> to display id with resource-type and version
	 * @return
	 * @throws NullPointerException
	 *             if the given <b>id</b> is <code>null</code>
	 */
	private static ElementId from(IIdType id, boolean fullId)
	{
		Objects.requireNonNull(id, "id");

		String href = fullId ? id.toUnqualified().getValue() : id.toVersionless().getValue();
		String resourceType = id.getResourceType();
		String value = fullId ? id.toUnqualified().getValue() : id.getIdPart();

		return new ElementId(href, resourceType, value);
	}

	public static ElementId from(String href, String resourceType, String value)
	{
		Objects.requireNonNull(href, "href");
		Objects.requireNonNull(resourceType, "resourceType");
		Objects.requireNonNull(value, "value");

		return new ElementId(href, resourceType, value);
	}

	private final String href;
	private final String resourceType;
	private final String value;

	private ElementId(String href, String resourceType, String value)
	{
		this.href = href;
		this.resourceType = resourceType;
		this.value = value;
	}

	public String getHref()
	{
		return href;
	}

	public String getResourceType()
	{
		return resourceType;
	}

	public String getValue()
	{
		return value;
	}
}
