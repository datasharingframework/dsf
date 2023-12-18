package dev.dsf.fhir.search.parameters;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.HealthcareService;

import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;

@SearchParameterDefinition(name = AbstractIdentifierParameter.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/HealthcareService-identifier", type = SearchParamType.TOKEN, documentation = "External identifiers for this item")
public class HealthcareServiceIdentifier extends AbstractIdentifierParameter<HealthcareService>
{
	public HealthcareServiceIdentifier()
	{
		super(HealthcareService.class, "healthcare_service",
				listMatcher(HealthcareService::hasIdentifier, HealthcareService::getIdentifier));
	}
}
