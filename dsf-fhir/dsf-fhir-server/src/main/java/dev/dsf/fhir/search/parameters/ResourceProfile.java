package dev.dsf.fhir.search.parameters;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractCanonicalUrlParameter;

@SearchParameterDefinition(name = ResourceProfile.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Resource-profile", type = SearchParamType.URI, documentation = "Profiles this resource claims to conform to")
public class ResourceProfile<R extends Resource> extends AbstractCanonicalUrlParameter<R>
{
	public static final String PARAMETER_NAME = "_profile";

	private final String resourceColumn;

	public ResourceProfile(Class<R> resourceType, String resourceColumn)
	{
		super(resourceType, PARAMETER_NAME);

		this.resourceColumn = resourceColumn;
	}

	@Override
	public String getFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case PRECISE -> resourceColumn + "->'meta'->'profile' ?? ?";

			case BELOW -> "EXISTS (SELECT 1 FROM (SELECT jsonb_array_elements_text(" + resourceColumn
					+ "->'meta'->'profile') AS profile) AS profiles WHERE profile LIKE ?)";

			default -> "";
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
			case PRECISE -> {
				if (valueAndType.version != null)
					statement.setString(parameterIndex, valueAndType.url + "|" + valueAndType.version);
				else
					statement.setString(parameterIndex, valueAndType.url);
			}

			case BELOW -> {
				if (valueAndType.version != null)
					statement.setString(parameterIndex, valueAndType.url + "|" + valueAndType.version + "%");
				else
					statement.setString(parameterIndex, valueAndType.url + "%");
			}

			default -> throw notDefined();
		}
	}

	@Override
	protected boolean resourceMatches(R resource)
	{
		return switch (valueAndType.type)
		{
			case PRECISE -> {
				if (valueAndType.version != null)
					yield resource.getMeta().getProfile().stream()
							.anyMatch(p -> p.getValue().equals(valueAndType.url + "|" + valueAndType.version));
				else
					yield resource.getMeta().getProfile().stream().anyMatch(p -> p.getValue().equals(valueAndType.url));
			}

			case BELOW -> {
				if (valueAndType.version != null)
					yield resource.getMeta().getProfile().stream()
							.anyMatch(p -> p.getValue().startsWith(valueAndType.url + "|" + valueAndType.version));
				else
					yield resource.getMeta().getProfile().stream()
							.anyMatch(p -> p.getValue().startsWith(valueAndType.url));
			}

			default -> throw notDefined();
		};
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return resourceColumn + "->'meta'->>'profile'" + sortDirectionWithSpacePrefix;
	}
}
