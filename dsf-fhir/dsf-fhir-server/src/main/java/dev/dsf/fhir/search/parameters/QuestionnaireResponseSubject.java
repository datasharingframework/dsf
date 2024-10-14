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

@IncludeParameterDefinition(resourceType = QuestionnaireResponse.class, parameterName = QuestionnaireResponseSubject.PARAMETER_NAME, targetResourceTypes = {
		Organization.class, Practitioner.class, PractitionerRole.class })
@SearchParameterDefinition(name = QuestionnaireResponseSubject.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/QuestionnaireResponse-subject", type = SearchParamType.REFERENCE, documentation = "The subject of the questionnaire response")
public class QuestionnaireResponseSubject extends AbstractReferenceParameter<QuestionnaireResponse>
{
	private static final String RESOURCE_TYPE_NAME = "QuestionnaireResponse";
	public static final String PARAMETER_NAME = "subject";

	// TODO if needed, modify for Reference(Any), see also doResolveReferencesForMatching, matches, getIncludeSql
	private static final String[] TARGET_RESOURCE_TYPE_NAMES = { "Organization", "Practitioner", "PractitionerRole" };

	public static List<String> getIncludeParameterValues()
	{
		return Arrays.stream(TARGET_RESOURCE_TYPE_NAMES)
				.map(target -> RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME + ":" + target).toList();
	}

	private static final String IDENTIFIERS_SUBQUERY = "(SELECT practitioner->'identifier' FROM current_practitioners "
			+ "WHERE concat('Practitioner/', practitioner->>'id') = questionnaire_response->'subject'->>'reference' "
			+ "UNION SELECT organization->'identifier' FROM current_organizations "
			+ "WHERE concat('Organization/', organization->>'id') = questionnaire_response->'subject'->>'reference' "
			+ "UNION SELECT practitioner_role->'identifier' FROM current_practitioner_roles "
			+ "WHERE concat('PractitionerRole/', practitioner_role->>'id') = questionnaire_response->'subject'->>'reference')";

	public QuestionnaireResponseSubject()
	{
		super(QuestionnaireResponse.class, PARAMETER_NAME, TARGET_RESOURCE_TYPE_NAMES);
	}

	@Override
	public String getFilterQuery()
	{
		return switch (valueAndType.type)
		{
			// testing all TargetResourceTypeName/ID combinations
			case ID -> "questionnaire_response->'subject'->>'reference' = ANY (?)";
			case RESOURCE_NAME_AND_ID, URL, TYPE_AND_ID, TYPE_AND_RESOURCE_NAME_AND_ID ->
				"questionnaire_response->'subject'->>'reference' = ?";
			case IDENTIFIER -> switch (valueAndType.identifier.type)
			{
				case CODE, CODE_AND_SYSTEM, SYSTEM -> IDENTIFIERS_SUBQUERY + " @> ?::jsonb";
				case CODE_AND_NO_SYSTEM_PROPERTY -> "(SELECT count(*) FROM jsonb_array_elements(" + IDENTIFIERS_SUBQUERY
						+ ") identifier WHERE identifier->>'value' = ? AND NOT (identifier ?? 'system')) > 0";
			};
		};
	}

	@Override
	public int getSqlParameterCount()
	{
		return 1;
	}

	@Override
	public void modifyStatement(int parameterIndex, int subqueryParameterIndex, PreparedStatement statement,
			BiFunctionWithSqlException<String, Object[], Array> arrayCreator) throws SQLException
	{
		switch (valueAndType.type)
		{
			case ID:
				Array array = arrayCreator.apply("TEXT",
						Arrays.stream(TARGET_RESOURCE_TYPE_NAMES).map(n -> n + "/" + valueAndType.id).toArray());
				statement.setArray(parameterIndex, array);
				break;
			case RESOURCE_NAME_AND_ID:
			case TYPE_AND_ID:
			case TYPE_AND_RESOURCE_NAME_AND_ID:
				statement.setString(parameterIndex, valueAndType.resourceName + "/" + valueAndType.id);
				break;
			case URL:
				statement.setString(parameterIndex, valueAndType.url);
				break;
			case IDENTIFIER:
			{
				switch (valueAndType.identifier.type)
				{
					case CODE:
						statement.setString(parameterIndex,
								"[{\"value\": \"" + valueAndType.identifier.codeValue + "\"}]");
						break;
					case CODE_AND_SYSTEM:
						statement.setString(parameterIndex, "[{\"value\": \"" + valueAndType.identifier.codeValue
								+ "\", \"system\": \"" + valueAndType.identifier.systemValue + "\"}]");
						break;
					case CODE_AND_NO_SYSTEM_PROPERTY:
						statement.setString(parameterIndex, valueAndType.identifier.codeValue);
						break;
					case SYSTEM:
						statement.setString(parameterIndex,
								"[{\"system\": \"" + valueAndType.identifier.systemValue + "\"}]");
						break;
				}
			}
		}
	}

	@Override
	protected void doResolveReferencesForMatching(QuestionnaireResponse resource, DaoProvider daoProvider)
			throws SQLException
	{
		Reference reference = resource.getSubject();
		IIdType idType = reference.getReferenceElement();

		if (idType.hasResourceType())
		{
			if ("Organization".equals(idType.getResourceType()))
				setResource(reference, idType, daoProvider.getOrganizationDao());
			else if ("Practitioner".equals(idType.getResourceType()))
				setResource(reference, idType, daoProvider.getPractitionerDao());
			else if ("PractitionerRole".equals(idType.getResourceType()))
				setResource(reference, idType, daoProvider.getPractitionerRoleDao());
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
			return switch (resource.getSubject().getResource())
			{
				case Organization o -> o.getIdentifier().stream()
						.anyMatch(AbstractIdentifierParameter.identifierMatches(valueAndType.identifier));
				case Practitioner p -> p.getIdentifier().stream()
						.anyMatch(AbstractIdentifierParameter.identifierMatches(valueAndType.identifier));
				case PractitionerRole r -> r.getIdentifier().stream()
						.anyMatch(AbstractIdentifierParameter.identifierMatches(valueAndType.identifier));
				default -> false;
			};
		}
		else
		{
			String ref = resource.getSubject().getReference();
			return switch (valueAndType.type)
			{
				case ID -> ref.equals("Organization" + "/" + valueAndType.id)
						|| ref.equals("Practitioner" + "/" + valueAndType.id)
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
		return "questionnaire_response->'subject'->>'reference'";
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
				case "Organization" -> "(SELECT jsonb_build_array(organization) FROM current_organizations"
						+ " WHERE concat('Organization/', organization->>'id') = questionnaire_response->'subject'->>'reference') AS organizations";
				case "Practitioner" -> "(SELECT jsonb_build_array(practitioner) FROM current_practitioners"
						+ " WHERE concat('Practitioner/', practitioner->>'id') = questionnaire_response->'subject'->>'reference') AS practitioners";
				case "PractitionerRole" ->
					"(SELECT jsonb_build_array(practitioner_role) FROM current_practitioner_roles"
							+ " WHERE concat('PractitionerRole/', practitioner_role->>'id') = questionnaire_response->'subject'->>'reference') AS practitioner_roles";
				default -> null;
			};
		else
			return null;
	}

	@Override
	protected void modifyIncludeResource(IncludeParts includeParts, Resource resource, Connection connection)
	{
		// Nothing to do for organizations, practitioners or practitioner-roles
	}
}
