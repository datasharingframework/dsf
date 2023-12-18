package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.HealthcareService;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractActiveParameter;

@SearchParameterDefinition(name = AbstractActiveParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/HealthcareService-active", type = SearchParamType.TOKEN, documentation = "The Healthcare Service is currently marked as active [true|false]")
public class HealthcareServiceActive extends AbstractActiveParameter<HealthcareService>
{
	public HealthcareServiceActive()
	{
		super(HealthcareService.class, "healthcare_service", HealthcareService::hasActive,
				HealthcareService::getActive);
	}
}
