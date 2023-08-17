package dev.dsf.fhir.search.parameters;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;
import org.postgresql.util.PGobject;

import ca.uhn.fhir.parser.DataFormatException;
import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQueryParameterError.SearchQueryParameterErrorType;
import dev.dsf.fhir.search.parameters.basic.AbstractSearchParameter;

@SearchParameterDefinition(name = ResourceId.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Resource-id", type = SearchParamType.STRING, documentation = "Logical id of this resource")
public class ResourceId<R extends Resource> extends AbstractSearchParameter<R>
{
	public static final String PARAMETER_NAME = "_id";

	private final String resourceIdColumn;
	private UUID id;

	public ResourceId(String resourceIdColumn)
	{
		super(PARAMETER_NAME);

		this.resourceIdColumn = resourceIdColumn;
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		id = toId(errors, queryParameterValue);
	}

	private UUID toId(List<? super SearchQueryParameterError> errors, String value)
	{
		if (value.isBlank())
		{
			errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE, PARAMETER_NAME,
					value));
			return null;
		}

		try
		{
			return UUID.fromString(value);
		}
		catch (IllegalArgumentException e)
		{
			errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE, PARAMETER_NAME,
					value, e));
			return null;
		}
	}

	@Override
	public boolean isDefined()
	{
		return id != null;
	}

	@Override
	public String getFilterQuery()
	{
		return resourceIdColumn + " = ?";
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
		statement.setObject(parameterIndex, asUuidPgObject(id));
	}

	private PGobject asUuidPgObject(UUID uuid)
	{
		if (uuid == null)
			return null;

		try
		{
			PGobject o = new PGobject();
			o.setType("UUID");
			o.setValue(uuid.toString());
			return o;
		}
		catch (DataFormatException | SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getBundleUriQueryParameterName()
	{
		return PARAMETER_NAME;
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return id.toString();
	}

	@Override
	public boolean matches(Resource resource)
	{
		if (isDefined())
			return Objects.equals(resource.getId(), id.toString());
		else
			throw notDefined();
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return resourceIdColumn + sortDirectionWithSpacePrefix;
	}
}
