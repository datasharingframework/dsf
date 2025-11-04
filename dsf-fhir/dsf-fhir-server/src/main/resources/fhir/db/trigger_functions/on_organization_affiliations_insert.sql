--
-- Copyright 2018-2025 Heilbronn University of Applied Sciences
--
-- Licensed under the Apache License, Version 2.0 (the "License");
-- you may not use this file except in compliance with the License.
-- You may obtain a copy of the License at
--
--     http://www.apache.org/licenses/LICENSE-2.0
--
-- Unless required by applicable law or agreed to in writing, software
-- distributed under the License is distributed on an "AS IS" BASIS,
-- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
-- See the License for the specific language governing permissions and
-- limitations under the License.
--

CREATE OR REPLACE FUNCTION on_organization_affiliations_insert() RETURNS TRIGGER AS $$
DECLARE
	organization_affiliation_exists_active_roles JSONB := (SELECT organization_affiliation->'code' FROM organization_affiliations WHERE organization_affiliation_id = NEW.organization_affiliation_id AND version = NEW.version - 1 AND deleted IS NULL AND organization_affiliation->>'active' = 'true');
	reference_regex TEXT := '((http|https):\/\/([A-Za-z0-9\-\\\.\:\%\$]*\/)+)?(Account|ActivityDefinition|AdverseEvent|AllergyIntolerance|Appointment|AppointmentResponse|AuditEvent|Basic|Binary|BiologicallyDerivedProduct|BodyStructure|Bundle|CapabilityStatement|CarePlan|CareTeam|CatalogEntry|ChargeItem|ChargeItemDefinition|Claim|ClaimResponse|ClinicalImpression|CodeSystem|Communication|CommunicationRequest|CompartmentDefinition|Composition|ConceptMap|Condition|Consent|Contract|Coverage|CoverageEligibilityRequest|CoverageEligibilityResponse|DetectedIssue|Device|DeviceDefinition|DeviceMetric|DeviceRequest|DeviceUseStatement|DiagnosticReport|DocumentManifest|DocumentReference|EffectEvidenceSynthesis|Encounter|Endpoint|EnrollmentRequest|EnrollmentResponse|EpisodeOfCare|EventDefinition|Evidence|EvidenceVariable|ExampleScenario|ExplanationOfBenefit|FamilyMemberHistory|Flag|Goal|GraphDefinition|Group|GuidanceResponse|HealthcareService|ImagingStudy|Immunization|ImmunizationEvaluation|ImmunizationRecommendation|ImplementationGuide|InsurancePlan|Invoice|Library|Linkage|List|Location|Measure|MeasureReport|Media|Medication|MedicationAdministration|MedicationDispense|MedicationKnowledge|MedicationRequest|MedicationStatement|MedicinalProduct|MedicinalProductAuthorization|MedicinalProductContraindication|MedicinalProductIndication|MedicinalProductIngredient|MedicinalProductInteraction|MedicinalProductManufactured|MedicinalProductPackaged|MedicinalProductPharmaceutical|MedicinalProductUndesirableEffect|MessageDefinition|MessageHeader|MolecularSequence|NamingSystem|NutritionOrder|Observation|ObservationDefinition|OperationDefinition|OperationOutcome|Organization|OrganizationAffiliation|Patient|PaymentNotice|PaymentReconciliation|Person|PlanDefinition|Practitioner|PractitionerRole|Procedure|Provenance|Questionnaire|QuestionnaireResponse|RelatedPerson|RequestGroup|ResearchDefinition|ResearchElementDefinition|ResearchStudy|ResearchSubject|RiskAssessment|RiskEvidenceSynthesis|Schedule|SearchParameter|ServiceRequest|Slot|Specimen|SpecimenDefinition|StructureDefinition|StructureMap|Subscription|Substance|SubstanceNucleicAcid|SubstancePolymer|SubstanceProtein|SubstanceReferenceInformation|SubstanceSourceMaterial|SubstanceSpecification|SupplyDelivery|SupplyRequest|Task|TerminologyCapabilities|TestReport|TestScript|ValueSet|VerificationResult|VisionPrescription)\/([A-Za-z0-9\-\.]{1,64})(\/_history\/([A-Za-z0-9\-\.]{1,64}))?';
	parent_organization_identifier TEXT;
	member_organization_id UUID;
	insert_count INT;
	binary_insert_count INT;
	delete_count INT;
BEGIN
	PERFORM on_resources_insert(NEW.organization_affiliation_id, NEW.version, NEW.organization_affiliation);

	IF ((NEW.organization_affiliation->>'active' = 'false') AND organization_affiliation_exists_active_roles IS NOT NULL)
	OR ((NEW.organization_affiliation->>'active' = 'true') AND organization_affiliation_exists_active_roles IS NOT NULL AND NEW.organization_affiliation->'code' <> organization_affiliation_exists_active_roles) THEN
		RAISE NOTICE 'new organization_affiliation inactive and old organization_affiliation exists and active -> delete';

		DELETE FROM read_access
		WHERE access_type = 'ROLE'
		AND organization_affiliation_id = NEW.organization_affiliation_id;

		GET DIAGNOSTICS delete_count = ROW_COUNT;
		RAISE NOTICE 'Existing rows deleted from read_access for created/updated organization-affiliation: %', delete_count;

	ELSIF ((NEW.organization_affiliation->>'active' = 'true') AND NOT organization_affiliation_exists_active_roles IS NOT NULL)
	OR ((NEW.organization_affiliation->>'active' = 'true') AND organization_affiliation_exists_active_roles IS NOT NULL AND NEW.organization_affiliation->'code' <> organization_affiliation_exists_active_roles) THEN
		RAISE NOTICE 'new organization_affiliation active and old organization_affiliation not exist or inactive -> insert';

		parent_organization_identifier := jsonb_path_query(organization, '$.identifier[*] ? (@.system == "http://dsf.dev/sid/organization-identifier")')->>'value'
			FROM current_organizations
			WHERE organization_id = (regexp_match(NEW.organization_affiliation->'organization'->>'reference', reference_regex))[5]::uuid
			AND organization->>'active' = 'true';
		member_organization_id := organization_id FROM current_organizations
			WHERE organization_id = (regexp_match(NEW.organization_affiliation->'participatingOrganization'->>'reference', reference_regex))[5]::uuid
			AND organization->>'active' = 'true';

		IF (parent_organization_identifier IS NOT NULL AND member_organization_id IS NOT NULL) THEN
			RAISE NOTICE 'parent_organization_identifier IS NOT NULL AND member_organization_id IS NOT NULL';
			INSERT INTO read_access 			
				SELECT DISTINCT r.id, r.version, 'ROLE', member_organization_id, NEW.organization_affiliation_id
				FROM (
					SELECT 
						coding->>'system' AS system
						, coding->>'code' AS code
					FROM (
						SELECT jsonb_array_elements(jsonb_array_elements(NEW.organization_affiliation->'code')->'coding') AS coding
					) AS codings
				) AS c
				LEFT JOIN (
					SELECT
						id
						, version
						, resource
					FROM all_read_access_resources
				) AS r
				ON r.resource->'meta'->'tag' @> 
					('[{"extension":[{"url":"http://dsf.dev/fhir/StructureDefinition/extension-read-access-parent-organization-role","extension":[{"url":"parent-organization","valueIdentifier":{"system":"http://dsf.dev/sid/organization-identifier","value":"'
					|| parent_organization_identifier || '"}},{"url":"organization-role","valueCoding":{"system":"'
					|| c.system || '","code":"'
					|| c.code || '"}}]}],"system":"http://dsf.dev/fhir/CodeSystem/read-access-tag","code":"ROLE"}]')::jsonb
				WHERE r.resource IS NOT NULL;

			GET DIAGNOSTICS insert_count = ROW_COUNT;
			RAISE NOTICE 'Rows inserted into read_access: %', insert_count;

			INSERT INTO read_access
				SELECT binary_id, version, access_type, organization_id, organization_affiliation_id
				FROM read_access, current_binaries
				WHERE organization_id = member_organization_id
				AND organization_affiliation_id = NEW.organization_affiliation_id
				AND access_type = 'ROLE'
				AND resource_id = (regexp_match(binary_json->'securityContext'->>'reference', reference_regex))[5]::uuid;

			GET DIAGNOSTICS binary_insert_count = ROW_COUNT;
			RAISE NOTICE 'Rows inserted into read_access based on Binary.securityContext: %', binary_insert_count;
		END IF;
	END IF;
	
	RETURN NEW;
END;
$$ LANGUAGE PLPGSQL