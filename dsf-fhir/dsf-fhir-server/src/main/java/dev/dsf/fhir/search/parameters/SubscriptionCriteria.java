package dev.dsf.fhir.search.parameters;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

import org.hl7.fhir.r4.model.Enumerations.SearchParamType;
import org.hl7.fhir.r4.model.Subscription;

import dev.dsf.fhir.function.BiFunctionWithSqlException;
import dev.dsf.fhir.search.SearchQueryParameter.SearchParameterDefinition;
import dev.dsf.fhir.search.parameters.basic.AbstractStringParameter;

@SearchParameterDefinition(name = SubscriptionCriteria.PARAMETER_NAME, definition = "http://hl7.org/fhir/SearchParameter/Subscription-criteria", type = SearchParamType.STRING, documentation = "The search rules used to determine when to send a notification (always matches exact)")
public class SubscriptionCriteria extends AbstractStringParameter<Subscription>
{
	public static final String PARAMETER_NAME = "criteria";

	public SubscriptionCriteria()
	{
		super(Subscription.class, PARAMETER_NAME);
	}

	@Override
	public String getFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case STARTS_WITH, CONTAINS -> "lower(subscription->>'criteria') LIKE ?";
			case EXACT -> "subscription->>'criteria' = ?";
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
	protected boolean resourceMatches(Subscription resource)
	{
		return resource.hasCriteria() && criteriaMatches(resource.getCriteria());
	}

	private boolean criteriaMatches(String criteria)
	{
		return switch (valueAndType.type)
		{
			case STARTS_WITH -> criteria.toLowerCase().startsWith(valueAndType.value.toLowerCase());
			case CONTAINS -> criteria.toLowerCase().contains(valueAndType.value.toLowerCase());
			case EXACT -> Objects.equals(criteria, valueAndType.value);
		};
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "subscription->>'criteria'" + sortDirectionWithSpacePrefix;
	}
}
