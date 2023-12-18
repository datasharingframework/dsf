package dev.dsf.fhir.search.parameters.basic;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.function.BiFunctionWithSqlException;

public class AbstractNameParameter<R extends Resource> extends AbstractStringParameter<R>
{
	public static final String PARAMETER_NAME = "name";

	private final String resourceColumn;
	private final Predicate<R> hasName;
	private final Function<R, String> getName;

	public AbstractNameParameter(Class<R> resourceType, String resourceColumn, Predicate<R> hasName,
			Function<R, String> getName)
	{
		super(resourceType, PARAMETER_NAME);

		this.resourceColumn = resourceColumn;
		this.hasName = hasName;
		this.getName = getName;
	}

	@Override
	public String getFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case STARTS_WITH, CONTAINS -> "lower(" + resourceColumn + "->>'name') LIKE ?";
			case EXACT -> resourceColumn + "->>'name' = ?";
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
			case STARTS_WITH:
				statement.setString(parameterIndex, valueAndType.value.toLowerCase() + "%");
				return;

			case CONTAINS:
				statement.setString(parameterIndex, "%" + valueAndType.value.toLowerCase() + "%");
				return;

			case EXACT:
				statement.setString(parameterIndex, valueAndType.value);
				return;
		}
	}

	@Override
	protected boolean resourceMatches(R resource)
	{
		return hasName.test(resource) && nameMatches(getName.apply(resource));
	}

	private boolean nameMatches(String name)
	{
		return switch (valueAndType.type)
		{
			case STARTS_WITH -> name.toLowerCase().startsWith(valueAndType.value.toLowerCase());
			case CONTAINS -> name.toLowerCase().contains(valueAndType.value.toLowerCase());
			case EXACT -> Objects.equals(name, valueAndType.value);
		};
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return resourceColumn + "->>'name'" + sortDirectionWithSpacePrefix;
	}
}
