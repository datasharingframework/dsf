CREATE OR REPLACE FUNCTION on_organizations_insert() RETURNS TRIGGER AS $$
DECLARE
	organization_exists_active BOOLEAN := EXISTS (SELECT 1 FROM organizations WHERE organization_id = NEW.organization_id AND version = NEW.version - 1 AND deleted IS NULL AND organization->>'active' = 'true');
	reference_regex TEXT := '((http|https):\/\/([A-Za-z0-9\-\\\.\:\%\$]*\/)+)?(Account|ActivityDefinition|AdverseEvent|AllergyIntolerance|Appointment|AppointmentResponse|AuditEvent|Basic|Binary|BiologicallyDerivedProduct|BodyStructure|Bundle|CapabilityStatement|CarePlan|CareTeam|CatalogEntry|ChargeItem|ChargeItemDefinition|Claim|ClaimResponse|ClinicalImpression|CodeSystem|Communication|CommunicationRequest|CompartmentDefinition|Composition|ConceptMap|Condition|Consent|Contract|Coverage|CoverageEligibilityRequest|CoverageEligibilityResponse|DetectedIssue|Device|DeviceDefinition|DeviceMetric|DeviceRequest|DeviceUseStatement|DiagnosticReport|DocumentManifest|DocumentReference|EffectEvidenceSynthesis|Encounter|Endpoint|EnrollmentRequest|EnrollmentResponse|EpisodeOfCare|EventDefinition|Evidence|EvidenceVariable|ExampleScenario|ExplanationOfBenefit|FamilyMemberHistory|Flag|Goal|GraphDefinition|Group|GuidanceResponse|HealthcareService|ImagingStudy|Immunization|ImmunizationEvaluation|ImmunizationRecommendation|ImplementationGuide|InsurancePlan|Invoice|Library|Linkage|List|Location|Measure|MeasureReport|Media|Medication|MedicationAdministration|MedicationDispense|MedicationKnowledge|MedicationRequest|MedicationStatement|MedicinalProduct|MedicinalProductAuthorization|MedicinalProductContraindication|MedicinalProductIndication|MedicinalProductIngredient|MedicinalProductInteraction|MedicinalProductManufactured|MedicinalProductPackaged|MedicinalProductPharmaceutical|MedicinalProductUndesirableEffect|MessageDefinition|MessageHeader|MolecularSequence|NamingSystem|NutritionOrder|Observation|ObservationDefinition|OperationDefinition|OperationOutcome|Organization|OrganizationAffiliation|Patient|PaymentNotice|PaymentReconciliation|Person|PlanDefinition|Practitioner|PractitionerRole|Procedure|Provenance|Questionnaire|QuestionnaireResponse|RelatedPerson|RequestGroup|ResearchDefinition|ResearchElementDefinition|ResearchStudy|ResearchSubject|RiskAssessment|RiskEvidenceSynthesis|Schedule|SearchParameter|ServiceRequest|Slot|Specimen|SpecimenDefinition|StructureDefinition|StructureMap|Subscription|Substance|SubstanceNucleicAcid|SubstancePolymer|SubstanceProtein|SubstanceReferenceInformation|SubstanceSourceMaterial|SubstanceSpecification|SupplyDelivery|SupplyRequest|Task|TerminologyCapabilities|TestReport|TestScript|ValueSet|VerificationResult|VisionPrescription)\/([A-Za-z0-9\-\.]{1,64})(\/_history\/([A-Za-z0-9\-\.]{1,64}))?';
	organization_identifier TEXT := jsonb_path_query(NEW.organization, '$.identifier[*]?(@.system == "http://dsf.dev/sid/organization-identifier")')->>'value';
	organization_insert_count INT;
	role_ids UUID[];
	binary_insert_count INT;
	delete_count INT;
	roles_delete_count INT;
BEGIN
	PERFORM on_resources_insert(NEW.organization_id, NEW.version, NEW.organization);

	IF (NEW.organization->>'active' = 'false') AND organization_exists_active THEN
		RAISE NOTICE 'new organization inactive and old organization exists and active -> delete';

		DELETE FROM read_access
		WHERE access_type = 'ORGANIZATION'
		AND organization_id = NEW.organization_id;

		GET DIAGNOSTICS delete_count = ROW_COUNT;
		RAISE NOTICE 'Existing rows deleted from read_access for created/updated organization, ORGANIZATION Tag: %', delete_count;

		DELETE FROM read_access
		WHERE access_type = 'ROLE'
		AND organization_affiliation_id IN (
			SELECT organization_affiliation_id FROM current_organization_affiliations 
			WHERE NEW.organization_id = (regexp_match(organization_affiliation->'participatingOrganization'->>'reference', reference_regex))[5]::uuid
			OR NEW.organization_id = (regexp_match(organization_affiliation->'organization'->>'reference', reference_regex))[5]::uuid
		); 

		GET DIAGNOSTICS roles_delete_count = ROW_COUNT;
		RAISE NOTICE 'Existing rows deleted from read_access for created/updated organization, ROLE Tag: %', roles_delete_count;

	ELSIF (NEW.organization->>'active' = 'true') AND NOT organization_exists_active THEN
		RAISE NOTICE 'new organization active and old organization not exist or inactive -> insert';
		
		INSERT INTO read_access
			SELECT id, version, 'ORGANIZATION', NEW.organization_id, NULL
			FROM all_read_access_resources
			WHERE jsonb_path_exists(resource,('$.meta.tag[*] ? (@.code == "ORGANIZATION" && @.system == "http://dsf.dev/fhir/CodeSystem/read-access-tag")
					.extension[*]?(@.url == "http://dsf.dev/fhir/StructureDefinition/extension-read-access-organization")
					.valueIdentifier[*]?(@.system == "http://dsf.dev/sid/organization-identifier" && @.value == "' || organization_identifier || '")')::jsonpath);

		GET DIAGNOSTICS organization_insert_count = ROW_COUNT;

		WITH temp_role_ids AS (		
		INSERT INTO read_access 			
			SELECT r.id, r.version, 'ROLE', member_organization_id, organization_affiliation_id FROM (
				SELECT DISTINCT  
					organization_affiliation_id
				 	, parent_organization_identifier
				 	, parent_organization_organization_id
				 	, member_organization_id
				 	, coding->>'system' AS coding_system
				 	, coding->>'code' AS coding_code
				FROM (
					SELECT
						organization_affiliation_id
						, (SELECT jsonb_path_query(organization, '$.identifier[*]?(@.system == "http://dsf.dev/sid/organization-identifier")')->>'value' FROM current_organizations WHERE 
						   organization_id = (regexp_match(organization_affiliation->'organization'->>'reference', reference_regex))[5]::uuid
						  ) AS parent_organization_identifier
					 	, (regexp_match(organization_affiliation->'organization'->>'reference', reference_regex))[5]::uuid
					 		AS parent_organization_organization_id
					 	, (regexp_match(organization_affiliation->'participatingOrganization'->>'reference', reference_regex))[5]::uuid
					 		AS member_organization_id
						, jsonb_array_elements(jsonb_array_elements(organization_affiliation->'code')->'coding') AS coding
						FROM current_organization_affiliations
						WHERE organization_affiliation->>'active' = 'true'
						AND (
							SELECT count(*) FROM current_organizations
							WHERE organization->>'active' = 'true'
							AND (organization_id = (regexp_match(organization_affiliation->'organization'->>'reference', reference_regex))[5]::uuid
							OR organization_id = (regexp_match(organization_affiliation->'participatingOrganization'->>'reference', reference_regex))[5]::uuid)
						) = 2
					) AS oa1
				WHERE parent_organization_organization_id = NEW.organization_id OR member_organization_id = NEW.organization_id
				) AS oa
				LEFT JOIN (
					SELECT id, version, resource FROM all_read_access_resources
				) AS r
				ON r.resource->'meta'->'tag' @> 
					('[{"extension":[{"url":"http://dsf.dev/fhir/StructureDefinition/extension-read-access-parent-organization-role","extension":[{"url":"parent-organization","valueIdentifier":{"system":"http://dsf.dev/sid/organization-identifier","value":"'
					|| parent_organization_identifier || '"}},{"url":"organization-role","valueCoding":{"system":"'
					|| coding_system || '","code":"'
					|| coding_code || '"}}]}],"system":"http://dsf.dev/fhir/CodeSystem/read-access-tag","code":"ROLE"}]')::jsonb
				WHERE r.resource IS NOT NULL
		RETURNING resource_id
		)
		SELECT array_agg(resource_id) FROM temp_role_ids INTO role_ids;

		RAISE NOTICE 'Rows inserted into read_acces for created/updated active organization: %', organization_insert_count + array_length(role_ids, 1);

		INSERT INTO read_access
			SELECT binary_id, version, access_type, organization_id, NULL
			FROM read_access, current_binaries
			WHERE access_type = 'ORGANIZATION'
			AND organization_id = NEW.organization_id
			AND resource_id = (regexp_match(binary_json->'securityContext'->>'reference', reference_regex))[5]::uuid
			UNION
			SELECT binary_id, version, access_type, organization_id, organization_affiliation_id
			FROM read_access, current_binaries
			WHERE access_type = 'ROLE'
			AND resource_id = ANY(role_ids)
			AND resource_id = (regexp_match(binary_json->'securityContext'->>'reference', reference_regex))[5]::uuid;

		GET DIAGNOSTICS binary_insert_count = ROW_COUNT;
		RAISE NOTICE 'Rows inserted into read_access based on Binary.securityContext: %', binary_insert_count;
	END IF;

	RETURN NEW;
END;
$$ LANGUAGE PLPGSQL