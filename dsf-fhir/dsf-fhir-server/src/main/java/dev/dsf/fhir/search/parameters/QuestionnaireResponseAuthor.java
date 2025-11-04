/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.fhir.search.parameters;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.dao.exception.ResourceDeletedException;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.IncludeParameterDefinition;
import dev.dsf.fhir.search.IncludeParts;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;
import dev.dsf.fhir.search.parameters.basic.AbstractReferenceParameter;

@IncludeParameterDefinition(resourceType = QuestionnaireResponse.class, parameterName = QuestionnaireResponseAuthor.PARAMETER_NAME, targetResourceTypes = {
		Practitioner.class, Organization.class, Patient.class, PractitionerRole.class })
@SearchParameterDefinition(name = QuestionnaireResponseAuthor.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/QuestionnaireResponse-author", type = SearchParamType.REFERENCE, documentation = "The author of the questionnaire response")
public class QuestionnaireResponseAuthor extends AbstractReferenceParameter<QuestionnaireResponse>
{
	private static final String RESOURCE_TYPE_NAME = "QuestionnaireResponse";
	public static final String PARAMETER_NAME = "author";
	private static final String[] TARGET_RESOURCE_TYPE_NAMES = { "Practitioner", "Organization", "Patient",
			"PractitionerRole" };
	// TODO add Device, RelatedPerson if supported, see also doResolveReferencesForMatching, matches, getIncludeSql

	public static List<String> getIncludeParameterValues()
	{
		return Arrays.stream(TARGET_RESOURCE_TYPE_NAMES)
				.map(target -> RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME + ":" + target).toList();
	}

	private static final String IDENTIFIERS_SUBQUERY = "(SELECT practitioner->'identifier' FROM current_practitioners "
			+ "WHERE concat('Practitioner/', practitioner->>'id') = questionnaire_response->'author'->>'reference' "
			+ "UNION SELECT organization->'identifier' FROM current_organizations "
			+ "WHERE concat('Organization/', organization->>'id') = questionnaire_response->'author'->>'reference' "
			+ "UNION SELECT patient->'identifier' FROM current_patients "
			+ "WHERE concat('Patient/', patient->>'id') = questionnaire_response->'author'->>'reference' "
			+ "UNION SELECT practitioner_role->'identifier' FROM current_practitioner_roles "
			+ "WHERE concat('PractitionerRole/', practitioner_role->>'id') = questionnaire_response->'author'->>'reference')";

	public QuestionnaireResponseAuthor()
	{
		super(QuestionnaireResponse.class, PARAMETER_NAME, TARGET_RESOURCE_TYPE_NAMES);
	}

	@Override
	public String getFilterQuery()
	{
		return switch (valueAndType.type)
		{
			// testing all TargetResourceTypeName/ID combinations
			case ID -> "questionnaire_response->'author'->>'reference' = ANY (?)";
			case RESOURCE_NAME_AND_ID, URL, TYPE_AND_ID, TYPE_AND_RESOURCE_NAME_AND_ID ->
				"questionnaire_response->'author'->>'reference' = ?";
			case IDENTIFIER -> switch (valueAndType.identifier.type)
			{
				case CODE -> "(" + IDENTIFIERS_SUBQUERY
						+ " @> ?::jsonb OR questionnaire_response->'author'->'identifier'->>'value' = ?)";
				case CODE_AND_SYSTEM -> "(" + IDENTIFIERS_SUBQUERY
						+ " @> ?::jsonb OR (questionnaire_response->'author'->'identifier'->>'system' = ? AND questionnaire_response->'author'->'identifier'->>'value' = ?))";
				case SYSTEM -> "(" + IDENTIFIERS_SUBQUERY
						+ " @> ?::jsonb OR questionnaire_response->'author'->'identifier'->>'system' = ?)";
				case CODE_AND_NO_SYSTEM_PROPERTY -> "((SELECT count(*) FROM jsonb_array_elements("
						+ IDENTIFIERS_SUBQUERY
						+ ") identifier WHERE identifier->>'value' = ? AND NOT (identifier ?? 'system')) > 0"
						+ " OR (questionnaire_response->'author'->'identifier'->>'system' = NULL AND questionnaire_response->'author'->'identifier'->>'value' = ?))";
			};
		};
	}

	@Override
	public int getSqlParameterCount()
	{
		return switch (valueAndType.type)
		{
			case ID, RESOURCE_NAME_AND_ID, URL, TYPE_AND_ID, TYPE_AND_RESOURCE_NAME_AND_ID -> 1;
			case IDENTIFIER -> switch (valueAndType.identifier.type)
			{
				case CODE, SYSTEM, CODE_AND_NO_SYSTEM_PROPERTY -> 2;
				case CODE_AND_SYSTEM -> 3;
			};
		};
	}

	@Override
	public void modifyStatement(int parameterIndex, int subqueryParameterIndex, PreparedStatement statement,
			BiFunctionWithSqlException<String, Object[], Array> arrayCreator) throws SQLException
	{
		switch (valueAndType.type)
		{
			case ID -> {
				Array array = arrayCreator.apply("TEXT",
						Arrays.stream(TARGET_RESOURCE_TYPE_NAMES).map(n -> n + "/" + valueAndType.id).toArray());
				statement.setArray(parameterIndex, array);
			}

			case RESOURCE_NAME_AND_ID, TYPE_AND_ID, TYPE_AND_RESOURCE_NAME_AND_ID ->
				statement.setString(parameterIndex, valueAndType.resourceName + "/" + valueAndType.id);

			case URL -> statement.setString(parameterIndex, valueAndType.url);

			case IDENTIFIER -> {
				switch (valueAndType.identifier.type)
				{
					case CODE -> {
						if (subqueryParameterIndex == 1)
							statement.setString(parameterIndex,
									"[{\"value\": \"" + valueAndType.identifier.codeValue + "\"}]");
						else if (subqueryParameterIndex == 2)
							statement.setString(parameterIndex, valueAndType.identifier.codeValue);
					}

					case CODE_AND_SYSTEM -> {
						if (subqueryParameterIndex == 1)
							statement.setString(parameterIndex, "[{\"system\": \"" + valueAndType.identifier.systemValue
									+ "\", \"value\": \"" + valueAndType.identifier.codeValue + "\"}]");
						else if (subqueryParameterIndex == 2)
							statement.setString(parameterIndex, valueAndType.identifier.systemValue);
						else if (subqueryParameterIndex == 3)
							statement.setString(parameterIndex, valueAndType.identifier.codeValue);
					}

					case SYSTEM -> {
						if (subqueryParameterIndex == 1)
							statement.setString(parameterIndex,
									"[{\"system\": \"" + valueAndType.identifier.systemValue + "\"}]");
						else if (subqueryParameterIndex == 2)
							statement.setString(parameterIndex, valueAndType.identifier.systemValue);
					}

					case CODE_AND_NO_SYSTEM_PROPERTY -> {
						if (subqueryParameterIndex == 1)
							statement.setString(parameterIndex, valueAndType.identifier.codeValue);
						else if (subqueryParameterIndex == 2)
							statement.setString(parameterIndex, valueAndType.identifier.codeValue);
					}
				}
			}
		}
	}

	@Override
	protected void doResolveReferencesForMatching(QuestionnaireResponse resource, DaoProvider daoProvider)
			throws SQLException
	{
		Reference reference = resource.getAuthor();
		if (reference != null)
		{
			IIdType idType = reference.getReferenceElement();

			if (idType.hasResourceType())
			{
				if ("Practitioner".equals(idType.getResourceType()))
					setResource(reference, idType, daoProvider.getPractitionerDao());
				else if ("Organization".equals(idType.getResourceType()))
					setResource(reference, idType, daoProvider.getOrganizationDao());
				else if ("Patient".equals(idType.getResourceType()))
					setResource(reference, idType, daoProvider.getPatientDao());
				else if ("PractitionerRole".equals(idType.getResourceType()))
					setResource(reference, idType, daoProvider.getPractitionerRoleDao());
			}
		}
	}

	private void setResource(Reference reference, IIdType idType, ResourceDao<?> dao) throws SQLException
	{
		try
		{
			if (idType.hasVersionIdPart())
				dao.readVersion(UUID.fromString(idType.getIdPart()), idType.getVersionIdPartAsLong())
						.ifPresent(reference::setResource);
			else
				dao.read(UUID.fromString(idType.getIdPart())).ifPresent(reference::setResource);
		}
		catch (ResourceDeletedException e)
		{
			// ignore while matching, will result in a non match if this would have been the matching resource
		}
	}

	@Override
	protected boolean resourceMatches(QuestionnaireResponse resource)
	{
		if (ReferenceSearchType.IDENTIFIER.equals(valueAndType.type))
		{
			return switch (resource.getAuthor().getResource())
			{
				case Practitioner p -> p.getIdentifier().stream()
						.anyMatch(AbstractIdentifierParameter.identifierMatches(valueAndType.identifier));
				case Organization o -> o.getIdentifier().stream()
						.anyMatch(AbstractIdentifierParameter.identifierMatches(valueAndType.identifier));
				case Patient p -> p.getIdentifier().stream()
						.anyMatch(AbstractIdentifierParameter.identifierMatches(valueAndType.identifier));
				case PractitionerRole r -> r.getIdentifier().stream()
						.anyMatch(AbstractIdentifierParameter.identifierMatches(valueAndType.identifier));
				default -> false;
			} || (resource.hasAuthor() && AbstractIdentifierParameter.identifierMatches(valueAndType.identifier,
					resource.getAuthor().getIdentifier()));
		}
		else
		{
			String ref = resource.getAuthor().getReference();
			return ref != null && switch (valueAndType.type)
			{
				case ID -> ref.equals("Practitioner" + "/" + valueAndType.id)
						|| ref.equals("Organization" + "/" + valueAndType.id)
						|| ref.equals("Patient" + "/" + valueAndType.id)
						|| ref.equals("PractitionerRole" + "/" + valueAndType.id);
				case RESOURCE_NAME_AND_ID -> ref.equals(valueAndType.resourceName + "/" + valueAndType.id);
				case URL -> ref.equals(valueAndType.url);
				default -> false;
			};
		}
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "questionnaire_response->'author'->>'reference'";
	}

	@Override
	protected String getIncludeSql(IncludeParts includeParts)
	{
		if (RESOURCE_TYPE_NAME.equals(includeParts.getSourceResourceTypeName())
				&& PARAMETER_NAME.equals(includeParts.getSearchParameterName())
				&& Arrays.stream(TARGET_RESOURCE_TYPE_NAMES)
						.anyMatch(n -> n.equals(includeParts.getTargetResourceTypeName())))

			return switch (includeParts.getTargetResourceTypeName())
			{
				case "Practitioner" -> "(SELECT jsonb_build_array(practitioner) FROM current_practitioners"
						+ " WHERE concat('Practitioner/', practitioner->>'id') = questionnaire_response->'author'->>'reference') AS practitioners";

				case "Organization" -> "(SELECT jsonb_build_array(organization) FROM current_organizations"
						+ " WHERE concat('Organization/', organization->>'id') = questionnaire_response->'author'->>'reference') AS organizations";

				case "Patient" -> "(SELECT jsonb_build_array(patient) FROM current_patients"
						+ " WHERE concat('Patient/', patient->>'id') = questionnaire_response->'author'->>'reference') AS patients";

				case "PractitionerRole" ->
					"(SELECT jsonb_build_array(practitioner_role) FROM current_practitioner_roles"
							+ " WHERE concat('PractitionerRole/', practitioner_role->>'id') = questionnaire_response->'author'->>'reference') AS practitioner_roles";

				default -> null;
			};
		else
			return null;
	}

	@Override
	protected void modifyIncludeResource(IncludeParts includeParts, Resource resource, Connection connection)
	{
		// Nothing to do for practitioners, organizations, patients or practitioner-roles
	}
}
