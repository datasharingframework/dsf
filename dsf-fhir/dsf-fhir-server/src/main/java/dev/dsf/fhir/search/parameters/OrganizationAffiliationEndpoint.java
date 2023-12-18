package dev.dsf.fhir.search.parameters;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Endpoint;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.OrganizationAffiliation;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.dao.EndpointDao;
import dev.dsf.fhir.dao.exception.ResourceDeletedException;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.IncludeParameterDefinition;
import dev.dsf.fhir.search.IncludeParts;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractIdentifierParameter;
import dev.dsf.fhir.search.parameters.basic.AbstractReferenceParameter;

@IncludeParameterDefinition(resourceType = OrganizationAffiliation.class, parameterName = OrganizationAffiliationEndpoint.PARAMETER_NAME, targetResourceTypes = Endpoint.class)
@SearchParameterDefinition(name = OrganizationAffiliationEndpoint.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/OrganizationAffiliation-endpoint", type = SearchParamType.REFERENCE, documentation = "Technical endpoints providing access to services operated for this role")
public class OrganizationAffiliationEndpoint extends AbstractReferenceParameter<OrganizationAffiliation>
{
	private static final String RESOURCE_TYPE_NAME = "OrganizationAffiliation";
	public static final String PARAMETER_NAME = "endpoint";
	private static final String TARGET_RESOURCE_TYPE_NAME = "Endpoint";

	public static List<String> getIncludeParameterValues()
	{
		return List.of(RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME,
				RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME + ":" + TARGET_RESOURCE_TYPE_NAME);
	}

	public OrganizationAffiliationEndpoint()
	{
		super(OrganizationAffiliation.class, PARAMETER_NAME, TARGET_RESOURCE_TYPE_NAME);
	}

	@Override
	public String getFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case ID, RESOURCE_NAME_AND_ID, URL, TYPE_AND_ID, TYPE_AND_RESOURCE_NAME_AND_ID ->
				"? IN (SELECT reference->>'reference' FROM jsonb_array_elements(organization_affiliation->'endpoint') AS reference)";
			case IDENTIFIER -> switch (valueAndType.identifier.type)
			{
				case CODE, CODE_AND_SYSTEM, SYSTEM ->
					"(SELECT jsonb_agg(identifier) FROM (SELECT identifier FROM current_endpoints, jsonb_array_elements(endpoint->'identifier') identifier"
							+ " WHERE concat('Endpoint/', endpoint->>'id') IN (SELECT reference->>'reference' FROM jsonb_array_elements(organization_affiliation->'endpoint') reference)"
							+ " ) AS identifiers) @> ?::jsonb";
				case CODE_AND_NO_SYSTEM_PROPERTY ->
					"(SELECT count(*) FROM (SELECT identifier FROM current_endpoints, jsonb_array_elements(endpoint->'identifier') identifier"
							+ " WHERE concat('Endpoint/', endpoint->>'id') IN (SELECT reference->>'reference' FROM jsonb_array_elements(organization_affiliation->'endpoint') reference)"
							+ " ) AS identifiers WHERE identifier->>'value' = ? AND NOT (identifier ?? 'system')) > 0";
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
	protected void doResolveReferencesForMatching(OrganizationAffiliation resource, DaoProvider daoProvider)
			throws SQLException
	{
		EndpointDao dao = daoProvider.getEndpointDao();
		for (Reference reference : resource.getEndpoint())
		{
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
	}

	@Override
	protected boolean resourceMatches(OrganizationAffiliation resource)
	{
		if (ReferenceSearchType.IDENTIFIER.equals(valueAndType.type))
		{
			return resource.getEndpoint().stream().map(Reference::getResource).filter(r -> r instanceof Endpoint)
					.map(r -> (Endpoint) r).map(Endpoint::getIdentifier).flatMap(List::stream)
					.anyMatch(AbstractIdentifierParameter.identifierMatches(valueAndType.identifier));
		}
		else
		{
			return resource.getEndpoint().stream().map(Reference::getReference).anyMatch(ref ->
			{
				return switch (valueAndType.type)
				{
					case ID -> ref.equals(TARGET_RESOURCE_TYPE_NAME + "/" + valueAndType.id);
					case RESOURCE_NAME_AND_ID -> ref.equals(valueAndType.resourceName + "/" + valueAndType.id);
					case URL -> ref.equals(valueAndType.url);
					default -> false;
				};
			});
		}
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "(SELECT string_agg(reference->>'reference', ' ') FROM jsonb_array_elements(organization_affiliation->'endpoint') AS reference)";
	}

	@Override
	protected String getIncludeSql(IncludeParts includeParts)
	{
		if (includeParts.matches(RESOURCE_TYPE_NAME, PARAMETER_NAME, TARGET_RESOURCE_TYPE_NAME))
			return "(SELECT jsonb_agg(endpoint) FROM current_endpoints WHERE concat('Endpoint/', endpoint->>'id') IN (SELECT reference->>'reference' FROM jsonb_array_elements(organization_affiliation->'endpoint') AS reference)) AS endpoints";
		else
			return null;
	}

	@Override
	protected void modifyIncludeResource(IncludeParts includeParts, Resource resource, Connection connection)
	{
		// Nothing to do for endpoints
	}
}
