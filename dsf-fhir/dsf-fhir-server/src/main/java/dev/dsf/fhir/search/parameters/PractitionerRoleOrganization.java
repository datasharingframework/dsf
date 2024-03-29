package dev.dsf.fhir.search.parameters;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Organization;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.dao.OrganizationDao;
import dev.dsf.fhir.dao.exception.ResourceDeletedException;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.IncludeParameterDefinition;
import dev.dsf.fhir.search.IncludeParts;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;
import dev.dsf.fhir.search.parameters.basic.AbstractReferenceParameter;

@IncludeParameterDefinition(resourceType = PractitionerRole.class, parameterName = PractitionerRoleOrganization.PARAMETER_NAME, targetResourceTypes = Organization.class)
@SearchParameterDefinition(name = PractitionerRoleOrganization.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/PractitionerRole-organization", type = SearchParamType.REFERENCE, documentation = "The identity of the organization the practitioner represents / acts on behalf of")
public class PractitionerRoleOrganization extends AbstractReferenceParameter<PractitionerRole>
{
	private static final String RESOURCE_TYPE_NAME = "PractitionerRole";
	public static final String PARAMETER_NAME = "organization";
	private static final String TARGET_RESOURCE_TYPE_NAME = "Organization";

	public static List<String> getIncludeParameterValues()
	{
		return List.of(RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME,
				RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME + ":" + TARGET_RESOURCE_TYPE_NAME);
	}

	private static final String PRACTITIONER_IDENTIFIERS_SUBQUERY = "(SELECT organization->'identifier' FROM current_organizations"
			+ " WHERE concat('Organization/', organization->>'id') = practitioner_role->'organization'->>'reference')";

	public PractitionerRoleOrganization()
	{
		super(PractitionerRole.class, PARAMETER_NAME, TARGET_RESOURCE_TYPE_NAME);
	}

	@Override
	public String getFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case ID, RESOURCE_NAME_AND_ID, URL, TYPE_AND_ID, TYPE_AND_RESOURCE_NAME_AND_ID ->
				"practitioner_role->'organization'->>'reference' = ?";
			case IDENTIFIER -> switch (valueAndType.identifier.type)
			{
				case CODE, CODE_AND_SYSTEM, SYSTEM -> PRACTITIONER_IDENTIFIERS_SUBQUERY + " @> ?::jsonb";
				case CODE_AND_NO_SYSTEM_PROPERTY ->
					"(SELECT count(*) FROM jsonb_array_elements(" + PRACTITIONER_IDENTIFIERS_SUBQUERY
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
			case RESOURCE_NAME_AND_ID:
			case TYPE_AND_ID:
			case TYPE_AND_RESOURCE_NAME_AND_ID:
				statement.setString(parameterIndex, TARGET_RESOURCE_TYPE_NAME + "/" + valueAndType.id);
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
	protected void doResolveReferencesForMatching(PractitionerRole resource, DaoProvider daoProvider)
			throws SQLException
	{
		OrganizationDao dao = daoProvider.getOrganizationDao();
		Reference reference = resource.getOrganization();
		IIdType idType = reference.getReferenceElement();

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
	protected boolean resourceMatches(PractitionerRole resource)
	{
		if (ReferenceSearchType.IDENTIFIER.equals(valueAndType.type))
		{
			if (resource.getOrganization().getResource() instanceof Organization o)
				return o.getIdentifier().stream()
						.anyMatch(AbstractIdentifierParameter.identifierMatches(valueAndType.identifier));
			else
				return false;
		}
		else
		{
			String ref = resource.getOrganization().getReference();
			return switch (valueAndType.type)
			{
				case ID -> ref.equals(TARGET_RESOURCE_TYPE_NAME + "/" + valueAndType.id);
				case RESOURCE_NAME_AND_ID -> ref.equals(valueAndType.resourceName + "/" + valueAndType.id);
				case URL -> ref.equals(valueAndType.url);
				default -> false;
			};
		}
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "practitioner_role->'organization'->>'reference'";
	}

	@Override
	protected String getIncludeSql(IncludeParts includeParts)
	{
		if (includeParts.matches(RESOURCE_TYPE_NAME, PARAMETER_NAME, TARGET_RESOURCE_TYPE_NAME))
			return "(SELECT jsonb_build_array(organization) FROM current_organizations WHERE concat('Organization/', organization->>'id') = practitioner_role->'organization'->>'reference') AS organizations";
		else
			return null;
	}

	@Override
	protected void modifyIncludeResource(IncludeParts includeParts, Resource resource, Connection connection)
	{
		// Nothing to do for organizations
	}
}
