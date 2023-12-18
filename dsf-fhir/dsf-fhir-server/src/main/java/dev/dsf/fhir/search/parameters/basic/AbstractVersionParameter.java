package dev.dsf.fhir.search.parameters.basic;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.hl7.fhir.r4.model.MetadataResource;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQueryParameterError.SearchQueryParameterErrorType;

public abstract class AbstractVersionParameter<R extends MetadataResource> extends AbstractTokenParameter<R>
{
	public static final String PARAMETER_NAME = "version";

	private final String resourceColumn;

	private String version;

	public AbstractVersionParameter(Class<R> resourceType, String resourceColumn)
	{
		super(resourceType, PARAMETER_NAME);

		this.resourceColumn = resourceColumn;
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		super.doConfigure(errors, queryParameterName, queryParameterValue);

		if (valueAndType != null && valueAndType.type == TokenSearchType.CODE)
			version = valueAndType.codeValue;
		else if (valueAndType != null)
			errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE, PARAMETER_NAME,
					queryParameterValue));
	}

	@Override
	public boolean isDefined()
	{
		return super.isDefined() && version != null;
	}

	@Override
	protected String getPositiveFilterQuery()
	{
		return resourceColumn + "->>'version' = ?";
	}

	@Override
	protected String getNegatedFilterQuery()
	{
		return resourceColumn + "->>'version' <> ?";
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
		statement.setString(parameterIndex, version);
	}

	@Override
	protected boolean resourceMatches(R resource)
	{
		return valueAndType.negated ^ (resource.hasVersion() && Objects.equals(resource.getVersion(), version));
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return resourceColumn + "->>'version'" + sortDirectionWithSpacePrefix;
	}
}
