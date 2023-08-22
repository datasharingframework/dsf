package dev.dsf.fhir.search.parameters;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Subscription;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.parameters.basic.AbstractTokenParameter;
import dev.dsf.fhir.search.parameters.basic.TokenSearchType;

@SearchParameterDefinition(name = SubscriptionPayload.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Subscription-payload", type = SearchParamType.TOKEN, documentation = "The mime-type of the notification payload")
public class SubscriptionPayload extends AbstractTokenParameter<Subscription>
{
	public static final String PARAMETER_NAME = "payload";
	public static final String RESOURCE_COLUMN = "subscription";

	private String payloadMimeType;

	public SubscriptionPayload()
	{
		super(PARAMETER_NAME);
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		super.doConfigure(errors, queryParameterName, queryParameterValue);

		if (valueAndType != null && valueAndType.type == TokenSearchType.CODE)
			payloadMimeType = valueAndType.codeValue;
	}

	@Override
	public boolean isDefined()
	{
		return super.isDefined() && payloadMimeType != null;
	}

	@Override
	public String getFilterQuery()
	{
		return RESOURCE_COLUMN + "->'channel'->>'payload' " + (valueAndType.negated ? "<>" : "=") + " ?";
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
		statement.setString(parameterIndex, payloadMimeType);
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return payloadMimeType;
	}

	@Override
	public boolean matches(Resource resource)
	{
		if (!isDefined())
			throw notDefined();

		if (!(resource instanceof Subscription))
			return false;

		if (valueAndType.negated)
			return !Objects.equals(((Subscription) resource).getChannel().getPayload(), payloadMimeType);
		else
			return Objects.equals(((Subscription) resource).getChannel().getPayload(), payloadMimeType);
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return RESOURCE_COLUMN + "->'channel'->>'payload'" + sortDirectionWithSpacePrefix;
	}
}
