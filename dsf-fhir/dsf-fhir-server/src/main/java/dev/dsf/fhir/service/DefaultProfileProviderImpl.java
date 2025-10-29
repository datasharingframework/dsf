package dev.dsf.fhir.service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.ResourceType;

public enum DefaultProfileProviderImpl implements DefaultProfileProvider
{
	ENABLED
	{
		@Override
		public void setDefaultProfile(Resource resource)
		{
			if (resource != null)
			{
				ResourceType resourceType = resource.getResourceType();

				List<String> oldProfiles = resource.getMeta().getProfile().stream().filter(CanonicalType::hasValue)
						.map(CanonicalType::getValue).toList();

				List<String> supportedDefaultProfiles = getSupportedDefaultProfiles(resourceType);

				if (Collections.disjoint(oldProfiles, supportedDefaultProfiles))
				{
					List<CanonicalType> newProfiles = Stream
							.concat(getDefaultProfile(resourceType).stream(), oldProfiles.stream())
							.filter(Objects::nonNull).distinct().map(CanonicalType::new).toList();

					resource.getMeta().setProfile(newProfiles);
				}
			}
		}
	},
	DISABLED
	{
		@Override
		public void setDefaultProfile(Resource resource)
		{
		}
	};

	@Override
	public Optional<String> getDefaultProfile(ResourceType resourceType)
	{
		String name = switch (resourceType)
		{
			case ActivityDefinition -> "activity-definition";
			case Binary -> "binary";
			case Bundle -> "bundle";
			case CodeSystem -> "code-system";
			case DocumentReference -> "document-reference";
			case Endpoint -> "endpoint";
			case Group -> "group";
			case HealthcareService -> "healthcare-service";
			case Library -> "library";
			case Location -> "location";
			case Measure -> "measure";
			case MeasureReport -> "measure-report";
			case NamingSystem -> "naming-system";
			case OrganizationAffiliation -> "organization-affiliation";
			case Organization -> "organization";
			case Patient -> "patient";
			case Practitioner -> "practitioner";
			case PractitionerRole -> "practitioner-role";
			case Provenance -> "provenance";
			case Questionnaire -> "questionnaire";
			case QuestionnaireResponse -> "questionnaire-response";
			case ResearchStudy -> "research-study";
			case StructureDefinition -> "structure-definition";
			case Subscription -> "subscription";
			case Task -> null;
			case ValueSet -> "value-set";

			default -> null;
		};

		return Optional.ofNullable(name).map("http://dsf.dev/fhir/StructureDefinition/"::concat);
	}

	@Override
	public List<String> getSupportedDefaultProfiles(ResourceType resourceType)
	{
		return Stream
				.concat(getDefaultProfile(resourceType).stream(), getSecondaryDefaultProfiles(resourceType).stream())
				.toList();
	}

	@Override
	public List<String> getSecondaryDefaultProfiles(ResourceType resourceType)
	{
		return switch (resourceType)
		{
			case Organization -> List.of("http://dsf.dev/fhir/StructureDefinition/organization-parent");

			default -> List.of();
		};
	}
}
