package dev.dsf.bpe.v2.variables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Resource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class FhirResourcesList
{
	private final List<Resource> resources = new ArrayList<>();

	@JsonCreator
	public FhirResourcesList(@JsonProperty("resources") Collection<? extends Resource> resources)
	{
		if (resources != null)
			this.resources.addAll(resources);
	}

	public FhirResourcesList(Resource... resources)
	{
		this(List.of(resources));
	}

	@JsonProperty("resources")
	public List<Resource> getResources()
	{
		return Collections.unmodifiableList(resources);
	}

	@SuppressWarnings("unchecked")
	@JsonIgnore
	public <R extends Resource> List<R> getResourcesAndCast()
	{
		return (List<R>) getResources();
	}

	@Override
	public String toString()
	{
		return "FhirResourcesList" + resources.stream().map(r -> r.getIdElement().toUnqualified().getValue())
				.collect(Collectors.joining(", ", "[", "]"));
	}
}
