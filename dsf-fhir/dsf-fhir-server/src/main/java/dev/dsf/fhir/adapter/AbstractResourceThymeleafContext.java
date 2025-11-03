package dev.dsf.fhir.adapter;

import java.util.Objects;
import java.util.function.BiConsumer;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.common.auth.conf.Identity;

abstract class AbstractResourceThymeleafContext<R extends Resource> extends AbstractThymeleafContext
{
	private final Class<R> resourceType;
	private final String htmlFragment;

	protected AbstractResourceThymeleafContext(Class<R> resourceType, String htmlFragment)
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
	public final void setVariables(BiConsumer<String, Object> variables, Resource resource, Identity identity)
	{
		if (resourceType.isInstance(resource))
			doSetVariables(variables, resourceType.cast(resource));
		else
			throw new IllegalStateException("Unsupported resource of type " + resource.getClass().getName()
					+ ", expected " + resourceType.getName());
	}

	protected abstract void doSetVariables(BiConsumer<String, Object> variables, R resource);
}
