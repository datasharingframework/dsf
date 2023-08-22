package dev.dsf.fhir.search.parameters;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQueryParameterError.SearchQueryParameterErrorType;
import dev.dsf.fhir.search.parameters.basic.AbstractTokenParameter;
import dev.dsf.fhir.search.parameters.basic.TokenSearchType;

@SearchParameterDefinition(name = TaskStatus.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Task-status", type = SearchParamType.TOKEN, documentation = "Search by task status")
public class TaskStatus extends AbstractTokenParameter<Task>
{
	public static final String PARAMETER_NAME = "status";
	public static final String RESOURCE_COLUMN = "task";

	private org.hl7.fhir.r4.model.Task.TaskStatus status;

	public TaskStatus()
	{
		super(PARAMETER_NAME);
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		super.doConfigure(errors, queryParameterName, queryParameterValue);

		if (valueAndType != null && valueAndType.type == TokenSearchType.CODE)
			status = toStatus(errors, valueAndType.codeValue, queryParameterValue);
	}

	private org.hl7.fhir.r4.model.Task.TaskStatus toStatus(List<? super SearchQueryParameterError> errors,
			String status, String queryParameterValue)
	{
		if (status == null || status.isBlank())
			return null;

		try
		{
			return org.hl7.fhir.r4.model.Task.TaskStatus.fromCode(status);
		}
		catch (FHIRException e)
		{
			errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE, parameterName,
					queryParameterValue, e));
			return null;
		}
	}

	@Override
	public boolean isDefined()
	{
		return super.isDefined() && status != null;
	}

	@Override
	public String getFilterQuery()
	{
		return RESOURCE_COLUMN + "->>'status' " + (valueAndType.negated ? "<>" : "=") + " ?";
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
		statement.setString(parameterIndex, status.toCode());
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return status.toCode();
	}

	@Override
	public boolean matches(Resource resource)
	{
		if (!isDefined())
			throw notDefined();

		if (!(resource instanceof Task))
			return false;

		if (valueAndType.negated)
			return !Objects.equals(((Task) resource).getStatus(), status);
		else
			return Objects.equals(((Task) resource).getStatus(), status);
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return RESOURCE_COLUMN + "->>'status'" + sortDirectionWithSpacePrefix;
	}
}
