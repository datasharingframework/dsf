package dev.dsf.bpe.v2.fhir;

import org.hl7.fhir.r4.model.ActivityDefinition;
import org.hl7.fhir.r4.model.CodeSystem;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.NamingSystem;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.ValueSet;

public abstract class AbstractFhirResourceModifier implements FhirResourceModifier
{
	@Override
	public ActivityDefinition modifyActivityDefinition(String filename, ActivityDefinition resource)
	{
		return resource;
	}

	@Override
	public CodeSystem modifyCodeSystem(String filename, CodeSystem resource)
	{
		return resource;
	}

	@Override
	public Library modifyLibrary(String filename, Library resource)
	{
		return resource;
	}

	@Override
	public Measure modifyMeasure(String filename, Measure resource)
	{
		return resource;
	}

	@Override
	public NamingSystem modifyNamingSystem(String filename, NamingSystem resource)
	{
		return resource;
	}

	@Override
	public Questionnaire modifyQuestionnaire(String filename, Questionnaire resource)
	{
		return resource;
	}

	@Override
	public StructureDefinition modifyStructureDefinition(String filename, StructureDefinition resource)
	{
		return resource;
	}

	@Override
	public Task modifyTask(String filename, Task resource)
	{
		return resource;
	}

	@Override
	public ValueSet modifyValueSet(String filename, ValueSet resource)
	{
		return resource;
	}
}
