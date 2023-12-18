package dev.dsf.fhir.search.parameters.basic;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.function.BiFunctionWithSqlException;

public abstract class AbstractIdentifierParameter<R extends Resource> extends AbstractTokenParameter<R>
{
	public static final String PARAMETER_NAME = "identifier";

	protected final String resourceColumn;

	private final BiPredicate<TokenValueAndSearchType, R> identifierMatches;

	public AbstractIdentifierParameter(Class<R> resourceType, String resourceColumn,
			BiPredicate<TokenValueAndSearchType, R> identifierMatches)
	{
		super(resourceType, PARAMETER_NAME);

		this.resourceColumn = resourceColumn;
		this.identifierMatches = identifierMatches;
	}

	protected static <R extends Resource> BiPredicate<TokenValueAndSearchType, R> listMatcher(
			Predicate<R> hasIdentifier, Function<R, List<Identifier>> getIdentifiers)
	{
		return (v, r) -> hasIdentifier.test(r) && getIdentifiers.apply(r).stream().anyMatch(identifierMatches(v));
	}

	protected static <R extends Resource> BiPredicate<TokenValueAndSearchType, R> singleMatcher(
			Predicate<R> hasIdentifier, Function<R, Identifier> getIdentifier)
	{
		return (v, r) -> hasIdentifier.test(r) && identifierMatches(v, getIdentifier.apply(r));
	}

	public static Predicate<Identifier> identifierMatches(TokenValueAndSearchType valueAndType)
	{
		return i -> identifierMatches(valueAndType, i);
	}

	public static boolean identifierMatches(TokenValueAndSearchType valueAndType, Identifier identifier)
	{
		return valueAndType.negated ^ switch (valueAndType.type)
		{
			case CODE -> identifier.hasValue() && Objects.equals(valueAndType.codeValue, identifier.getValue());

			case CODE_AND_SYSTEM ->
				identifier.hasValue() && Objects.equals(valueAndType.codeValue, identifier.getValue())
						&& identifier.hasSystem() && Objects.equals(valueAndType.systemValue, identifier.getSystem());

			case CODE_AND_NO_SYSTEM_PROPERTY -> identifier.hasValue()
					&& Objects.equals(valueAndType.codeValue, identifier.getValue()) && !identifier.hasSystem();

			case SYSTEM -> Objects.equals(valueAndType.systemValue, identifier.getSystem());

			default -> false;
		};
	}

	@Override
	protected String getPositiveFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case CODE, CODE_AND_SYSTEM, SYSTEM -> resourceColumn + "->'identifier' @> ?::jsonb";
			case CODE_AND_NO_SYSTEM_PROPERTY -> "(SELECT count(*) FROM jsonb_array_elements(" + resourceColumn
					+ "->'identifier') identifier WHERE identifier->>'value' = ? AND NOT (identifier ?? 'system')) > 0";
		};
	}

	@Override
	protected String getNegatedFilterQuery()
	{
		return switch (valueAndType.type)
		{
			case CODE, CODE_AND_SYSTEM, SYSTEM -> "NOT (" + resourceColumn + "->'identifier' @> ?::jsonb)";
			case CODE_AND_NO_SYSTEM_PROPERTY -> "(SELECT count(*) FROM jsonb_array_elements(" + resourceColumn
					+ "->'identifier') identifier WHERE identifier->>'value' <> ? OR (identifier ?? 'system')) > 0";
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
			case CODE:
				statement.setString(parameterIndex, "[{\"value\": \"" + valueAndType.codeValue + "\"}]");
				return;
			case CODE_AND_SYSTEM:
				statement.setString(parameterIndex, "[{\"value\": \"" + valueAndType.codeValue + "\", \"system\": \""
						+ valueAndType.systemValue + "\"}]");
				return;
			case CODE_AND_NO_SYSTEM_PROPERTY:
				statement.setString(parameterIndex, valueAndType.codeValue);
				return;
			case SYSTEM:
				statement.setString(parameterIndex, "[{\"system\": \"" + valueAndType.systemValue + "\"}]");
				return;
		}
	}

	@Override
	protected String getSortSql(String sortDirectionWithSpacePrefix)
	{
		return "(SELECT string_agg((identifier->>'system')::text || (identifier->>'value')::text, ' ') FROM jsonb_array_elements("
				+ resourceColumn + "->'identifier') identifier)" + sortDirectionWithSpacePrefix;
	}

	@Override
	protected boolean resourceMatches(R resource)
	{
		return identifierMatches.test(valueAndType, resource);
	}
}
