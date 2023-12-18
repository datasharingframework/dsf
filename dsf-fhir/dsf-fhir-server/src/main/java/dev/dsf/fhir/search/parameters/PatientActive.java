package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Patient;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractActiveParameter;

@SearchParameterDefinition(name = AbstractActiveParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Patient-active", type = SearchParamType.TOKEN, documentation = "Whether the patient record is active [true|false]")
public class PatientActive extends AbstractActiveParameter<Patient>
{
	public PatientActive()
	{
		super(Patient.class, "patient", Patient::hasActive, Patient::getActive);
	}
}
