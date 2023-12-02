package dev.dsf.fhir.search.parameters.basic;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.hl7.fhir.r4.model.MetadataResource;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQueryParameterError.SearchQueryParameterErrorType;

public abstract class AbstractVersionParameter<R extends MetadataResource> extends AbstractTokenParameter<R>
{
	public static final String PARAMETER_NAME = "version";

	private final String resourceColumn;

	private String version;

	public AbstractVersionParameter(String resourceColumn)
	{
		super(PARAMETER_NAME);

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
	public String getFilterQuery()
	{
		return resourceColumn + "->>'version' " + (valueAndType.negated ? "<>" : "=") + " ?";
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

	protected abstract boolean instanceOf(Resource resource);

	@Override
	public boolean matches(Resource resource)
	{
		if (!instanceOf(resource))
			return false;

		MetadataResource mRes = (MetadataResource) resource;

		if (valueAndType.negated)
			return !Objects.equals(mRes.getVersion(), version);
		else
			return Objects.equals(mRes.getVersion(), version);
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return resourceColumn + "->>'version'" + sortDirectionWithSpacePrefix;
	}
}
