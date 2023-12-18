package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.HealthcareService;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractNameParameter;

@SearchParameterDefinition(name = AbstractNameParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/HealthcareService-name", type = SearchParamType.STRING, documentation = "A portion of the Healthcare service name")
public class HealthcareServiceName extends AbstractNameParameter<HealthcareService>
{
	public HealthcareServiceName()
	{
		super(HealthcareService.class, "healthcare_service", HealthcareService::hasName, HealthcareService::getName);
	}
}
