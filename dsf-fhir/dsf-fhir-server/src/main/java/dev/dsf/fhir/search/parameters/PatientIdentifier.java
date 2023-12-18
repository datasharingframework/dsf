package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Patient;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Patient-identifier", type = SearchParamType.TOKEN, documentation = "A patient identifier")
public class PatientIdentifier extends AbstractIdentifierParameter<Patient>
{
	public PatientIdentifier()
	{
		super(Patient.class, "patient", listMatcher(Patient::hasIdentifier, Patient::getIdentifier));
	}
}
