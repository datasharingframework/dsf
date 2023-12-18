package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Practitioner;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractActiveParameter;

@SearchParameterDefinition(name = AbstractActiveParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Practitioner-active", type = SearchParamType.TOKEN, documentation = "Whether the practitioner record is active [true|false]")
public class PractitionerActive extends AbstractActiveParameter<Practitioner>
{
	public PractitionerActive()
	{
		super(Practitioner.class, "practitioner", Practitioner::hasActive, Practitioner::getActive);
	}
}
