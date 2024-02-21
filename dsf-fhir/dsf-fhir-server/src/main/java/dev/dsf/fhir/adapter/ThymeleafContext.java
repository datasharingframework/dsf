package dev.dsf.fhir.adapter;

import java.util.function.BiConsumer;

import org.hl7.fhir.r4.model.Resource;

public interface ThymeleafContext
{
	Class<? extends Resource> getResourceType();

	boolean isResourceSupported(String requestPathLastElement);

	String getHtmlFragment();

	void setVariables(BiConsumer<String, Object> variables, Resource resource);
}
