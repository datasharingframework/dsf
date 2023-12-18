package dev.dsf.fhir.search.parameters.basic;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.function.BiFunctionWithSqlException;

public class AbstractActiveParameter<R extends Resource> extends AbstractBooleanParameter<R>
{
	public static final String PARAMETER_NAME = "active";

	private final String resourceColumn;

	public AbstractActiveParameter(Class<R> resourceType, String resourceColumn, Predicate<R> hasBoolean,
			Function<R, Boolean> getBoolean)
	{
		super(resourceType, PARAMETER_NAME, hasBoolean, getBoolean);

		this.resourceColumn = resourceColumn;
	}

	@Override
	public String getFilterQuery()
	{
		return "(" + resourceColumn + "->>'active')::BOOLEAN = ?";
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
		statement.setBoolean(parameterIndex, value);
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "(" + resourceColumn + "->>'active')::BOOLEAN" + sortDirectionWithSpacePrefix;
	}
}
