package dev.dsf.fhir.adapter;

import java.security.Principal;
import java.util.function.BiConsumer;

import org.hl7.fhir.r4.model.Resource;

public interface ThymeleafContext
{
	Class<? extends Resource> getResourceType();

	boolean isResourceSupported(String requestPathLastElement);

	default boolean isRootSupported(Resource resource, Principal principal)
	{
		return false;
	}

	String getHtmlFragment();

	void setVariables(BiConsumer<String, Object> variables, Resource resource);
}
