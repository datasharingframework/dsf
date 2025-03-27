package dev.dsf.bpe.api.plugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FhirResourceModifiers implements FhirResourceModifier
{
	private final List<FhirResourceModifier> fhirResourceModifiers = new ArrayList<>();

	public FhirResourceModifiers(Collection<? extends FhirResourceModifier> fhirResourceModifiers)
	{
		if (fhirResourceModifiers != null)
			this.fhirResourceModifiers.addAll(fhirResourceModifiers);
	}

	@Override
	public Object modifyActivityDefinition(String filename, Object resource)
	{
		for (FhirResourceModifier m : fhirResourceModifiers)
			resource = m.modifyActivityDefinition(filename, resource);

		return resource;
	}

	@Override
	public Object modifyCodeSystem(String filename, Object resource)
	{
		for (FhirResourceModifier m : fhirResourceModifiers)
			resource = m.modifyCodeSystem(filename, resource);

		return resource;
	}

	@Override
	public Object modifyLibrary(String filename, Object resource)
	{
		for (FhirResourceModifier m : fhirResourceModifiers)
			resource = m.modifyLibrary(filename, resource);

		return resource;
	}

	@Override
	public Object modifyMeasure(String filename, Object resource)
	{
		for (FhirResourceModifier m : fhirResourceModifiers)
			resource = m.modifyMeasure(filename, resource);

		return resource;
	}

	@Override
	public Object modifyNamingSystem(String filename, Object resource)
	{
		for (FhirResourceModifier m : fhirResourceModifiers)
			resource = m.modifyNamingSystem(filename, resource);

		return resource;
	}

	@Override
	public Object modifyQuestionnaire(String filename, Object resource)
	{
		for (FhirResourceModifier m : fhirResourceModifiers)
			resource = m.modifyQuestionnaire(filename, resource);

		return resource;
	}

	@Override
	public Object modifyStructureDefinition(String filename, Object resource)
	{
		for (FhirResourceModifier m : fhirResourceModifiers)
			resource = m.modifyStructureDefinition(filename, resource);

		return resource;
	}

	@Override
	public Object modifyTask(String filename, Object resource)
	{
		for (FhirResourceModifier m : fhirResourceModifiers)
			resource = m.modifyTask(filename, resource);

		return resource;
	}

	@Override
	public Object modifyValueSet(String filename, Object resource)
	{
		for (FhirResourceModifier m : fhirResourceModifiers)
			resource = m.modifyValueSet(filename, resource);

		return resource;
	}
}
