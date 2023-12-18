package dev.dsf.fhir.search.parameters.basic;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import org.hl7.fhir.r4.model.MetadataResource;

import dev.dsf.fhir.function.BiFunctionWithSqlException;

public abstract class AbstractUrlAndVersionParameter<R extends MetadataResource>
		extends AbstractCanonicalUrlParameter<R>
{
	public static final String PARAMETER_NAME = "url";

	private final String resourceColumn;

	public AbstractUrlAndVersionParameter(Class<R> resourceType, String resourceColumn)
	{
		super(resourceType, PARAMETER_NAME);

		this.resourceColumn = resourceColumn;
	}

	@Override
	public String getFilterQuery()
	{
		String versionSubQuery = hasVersion() ? " AND " + resourceColumn + "->>'version' = ?" : "";

		return switch (valueAndType.type)
		{
			case PRECISE -> resourceColumn + "->>'url' = ?" + versionSubQuery;
			case BELOW -> resourceColumn + "->>'url' LIKE ?" + versionSubQuery;
		};
	}

	@Override
	public int getSqlParameterCount()
	{
		return hasVersion() ? 2 : 1;
	}

	@Override
	public void modifyStatement(int parameterIndex, int subqueryParameterIndex, PreparedStatement statement,
			BiFunctionWithSqlException<String, Object[], Array> arrayCreator) throws SQLException
	{
		if (subqueryParameterIndex == 1)
		{
			switch (valueAndType.type)
			{
				case PRECISE:
					statement.setString(parameterIndex, valueAndType.url);
					return;

				case BELOW:
					statement.setString(parameterIndex, valueAndType.url + "%");
					return;
			}
		}
		else if (subqueryParameterIndex == 2)
			statement.setString(parameterIndex, valueAndType.version);
	}

	@Override
	protected boolean resourceMatches(R resource)
	{
		return switch (valueAndType.type)
		{
			case PRECISE -> resource.hasUrl() && Objects.equals(resource.getUrl(), valueAndType.url)
					&& (valueAndType.version == null
							|| (resource.hasVersion() && Objects.equals(resource.getVersion(), valueAndType.version)));

			case BELOW ->
				resource.hasUrl() && resource.getUrl().startsWith(valueAndType.url) && (valueAndType.version == null
						|| (resource.hasVersion() && Objects.equals(resource.getVersion(), valueAndType.version)));
		};
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return resourceColumn + "->>'url'" + sortDirectionWithSpacePrefix;
	}
}
