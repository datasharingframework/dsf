package dev.dsf.maven.bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.ValueSet;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;

public class ValidationSupportWithCustomResources implements IValidationSupport
{
	private final FhirContext context;

	private final Map<String, StructureDefinition> structureDefinitionsByUrl = new HashMap<>();
	private final Map<String, CodeSystem> codeSystemsByUrl = new HashMap<>();
	private final Map<String, ValueSet> valueSetsByUrl = new HashMap<>();

	public ValidationSupportWithCustomResources(FhirContext context, Bundle bundle)
	{
		this.context = context;

		bundle.getEntry().stream().map(BundleEntryComponent::getResource)
				.filter(r -> r instanceof StructureDefinition || r instanceof CodeSystem || r instanceof ValueSet)
				.forEach(r ->
				{
					if (r instanceof StructureDefinition s)
					{
						structureDefinitionsByUrl.put(s.getUrl(), s);

						if (s.hasVersion())
							structureDefinitionsByUrl.put(s.getUrl() + "|" + s.getVersion(), s);
					}
					else if (r instanceof CodeSystem c)
					{
						codeSystemsByUrl.put(c.getUrl(), c);

						if (c.hasVersion())
							codeSystemsByUrl.put(c.getUrl() + "|" + c.getVersion(), c);
					}
					else if (r instanceof ValueSet v)
					{
						valueSetsByUrl.put(v.getUrl(), v);

						if (v.hasVersion())
							valueSetsByUrl.put(v.getUrl() + "|" + v.getVersion(), v);
					}
				});
	}

	@Override
	public FhirContext getFhirContext()
	{
		return context;
	}

	@Override
	public <T extends IBaseResource> List<T> fetchAllStructureDefinitions()
	{
		@SuppressWarnings("unchecked")
		List<T> definitions = (List<T>) new ArrayList<>(structureDefinitionsByUrl.values());

		return definitions;
	}

	@Override
	public StructureDefinition fetchStructureDefinition(String url)
	{
		return structureDefinitionsByUrl.getOrDefault(url, null);
	}

	public void addOrReplace(StructureDefinition s)
	{
		structureDefinitionsByUrl.put(s.getUrl(), s);
	}

	@Override
	public CodeSystem fetchCodeSystem(String url)
	{
		return codeSystemsByUrl.getOrDefault(url, null);
	}

	public void addOrReplace(CodeSystem s)
	{
		codeSystemsByUrl.put(s.getUrl(), s);
	}

	@Override
	public ValueSet fetchValueSet(String url)
	{
		return valueSetsByUrl.getOrDefault(url, null);
	}

	public void addOrReplace(ValueSet s)
	{
		valueSetsByUrl.put(s.getUrl(), s);
	}
}
