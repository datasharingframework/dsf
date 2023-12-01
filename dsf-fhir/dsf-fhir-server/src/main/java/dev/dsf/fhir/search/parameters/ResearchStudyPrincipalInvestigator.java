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
import org.hl7.fhir.r4.model.Practitioner;
import org.hl7.fhir.r4.model.PractitionerRole;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.ResearchStudy;
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

@IncludeParameterDefinition(resourceType = ResearchStudy.class, parameterName = ResearchStudyPrincipalInvestigator.PARAMETER_NAME, targetResourceTypes = {
		Practitioner.class, PractitionerRole.class })
@SearchParameterDefinition(name = ResearchStudyPrincipalInvestigator.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/ResearchStudy-principalinvestigator", type = SearchParamType.REFERENCE, documentation = "Researcher who oversees multiple aspects of the study")
public class ResearchStudyPrincipalInvestigator extends AbstractReferenceParameter<ResearchStudy>
{
	public static final String RESOURCE_TYPE_NAME = "ResearchStudy";
	public static final String PARAMETER_NAME = "principalinvestigator";
	public static final String[] TARGET_RESOURCE_TYPE_NAMES = { "Practitioner", "PractitionerRole" };

	public static List<String> getIncludeParameterValues()
	{
		return Arrays.stream(TARGET_RESOURCE_TYPE_NAMES)
				.map(target -> RESOURCE_TYPE_NAME + ":" + PARAMETER_NAME + ":" + target).toList();
	}

	private static final String IDENTIFIERS_SUBQUERY = "(SELECT practitioner->'identifier' FROM current_practitioners "
			+ "WHERE concat('Practitioner/', practitioner->>'id') = research_study->'principalInvestigator'->>'reference' "
			+ "UNION SELECT practitioner_role->'identifier' FROM current_practitioner_roles "
			+ "WHERE concat('PractitionerRole/', practitioner_role->>'id') = research_study->'principalInvestigator'->>'reference')";

	public ResearchStudyPrincipalInvestigator()
	{
		super(ResearchStudy.class, PARAMETER_NAME, TARGET_RESOURCE_TYPE_NAMES);
	}

	@Override
	public String getFilterQuery()
	{
		switch (valueAndType.type)
		{
			case ID:
				return "research_study->'principalInvestigator'->>'reference' = ANY (?)";
			case RESOURCE_NAME_AND_ID:
			case URL:
			case TYPE_AND_ID:
			case TYPE_AND_RESOURCE_NAME_AND_ID:
				return "research_study->'principalInvestigator'->>'reference' = ?";
			case IDENTIFIER:
			{
				switch (valueAndType.identifier.type)
				{
					case CODE:
					case CODE_AND_SYSTEM:
					case SYSTEM:
						return IDENTIFIERS_SUBQUERY + " @> ?::jsonb";
					case CODE_AND_NO_SYSTEM_PROPERTY:
						return "(SELECT count(*) FROM jsonb_array_elements(" + IDENTIFIERS_SUBQUERY
								+ ") identifier WHERE identifier->>'value' = ? AND NOT (identifier ?? 'system')) > 0";
				}
			}
		}

		return "";
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
	protected void doResolveReferencesForMatching(ResearchStudy resource, DaoProvider daoProvider) throws SQLException
	{
		Reference reference = resource.getPrincipalInvestigator();
		IIdType idType = reference.getReferenceElement();

		if (idType.hasResourceType())
		{
			if ("Practitioner".equals(idType.getResourceType()))
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
	public boolean matches(Resource resource)
	{
		if (!isDefined())
			throw notDefined();

		if (!(resource instanceof ResearchStudy))
			return false;

		ResearchStudy r = (ResearchStudy) resource;

		if (ReferenceSearchType.IDENTIFIER.equals(valueAndType.type))
		{
			if (r.getPrincipalInvestigator().getResource() instanceof Practitioner p)
			{
				return p.getIdentifier().stream()
						.anyMatch(i -> AbstractIdentifierParameter.identifierMatches(valueAndType.identifier, i));
			}
			else if (r.getPrincipalInvestigator().getResource() instanceof PractitionerRole p)
			{
				return p.getIdentifier().stream()
						.anyMatch(i -> AbstractIdentifierParameter.identifierMatches(valueAndType.identifier, i));
			}
			else
				return false;
		}
		else
		{
			String ref = r.getPrincipalInvestigator().getReference();
			switch (valueAndType.type)
			{
				case ID:
					return ref.equals("Practitioner" + "/" + valueAndType.id)
							|| ref.equals("PractitionerRole" + "/" + valueAndType.id);
				case RESOURCE_NAME_AND_ID:
					return ref.equals(valueAndType.resourceName + "/" + valueAndType.id);
				case URL:
					return ref.equals(valueAndType.url);
				default:
					return false;
			}
		}
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "research_study->'principalInvestigator'->>'reference'";
	}

	@Override
	protected String getIncludeSql(IncludeParts includeParts)
	{
		if (RESOURCE_TYPE_NAME.equals(includeParts.getSourceResourceTypeName())
				&& PARAMETER_NAME.equals(includeParts.getSearchParameterName())
				&& Arrays.stream(TARGET_RESOURCE_TYPE_NAMES)
						.anyMatch(n -> n.equals(includeParts.getTargetResourceTypeName())))
			switch (includeParts.getTargetResourceTypeName())
			{
				case "Practitioner":
					return "(SELECT jsonb_build_array(practitioner) FROM current_practitioners"
							+ " WHERE concat('Practitioner/', practitioner->>'id') = research_study->'principalInvestigator'->>'reference') AS practitioners";
				case "PractitionerRole":
					return "(SELECT jsonb_build_array(practitioner_role) FROM current_practitioner_roles"
							+ " WHERE concat('PractitionerRole/', practitioner_role->>'id') = research_study->'principalInvestigator'->>'reference') AS practitioner_roles";
				default:
					return null;
			}
		else
			return null;
	}

	@Override
	protected void modifyIncludeResource(IncludeParts includeParts, Resource resource, Connection connection)
	{
		// Nothing to do for practitioners
	}
}
