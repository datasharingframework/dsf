package dev.dsf.fhir.search.parameters.basic;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.search.IncludeParts;
import dev.dsf.fhir.search.SearchQueryIncludeParameter;
import dev.dsf.fhir.search.SearchQueryIncludeParameterConfiguration;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.search.SearchQueryParameterError.SearchQueryParameterErrorType;

public abstract class AbstractReferenceParameter<R extends DomainResource> extends AbstractSearchParameter<R>
		implements SearchQueryIncludeParameter<R>
{
	public static final String PARAMETER_NAME_IDENTIFIER_MODIFIER = ":identifier";

	public static List<String> getNameModifiers()
	{
		return Collections.singletonList(PARAMETER_NAME_IDENTIFIER_MODIFIER);
	}

	protected static enum ReferenceSearchType
	{
		ID, TYPE_AND_ID, RESOURCE_NAME_AND_ID, TYPE_AND_RESOURCE_NAME_AND_ID, URL, IDENTIFIER
	}

	protected static class ReferenceValueAndSearchType
	{
		public final String resourceName;
		public final String id;
		public final String url;
		public final TokenValueAndSearchType identifier;

		public final ReferenceSearchType type;

		private ReferenceValueAndSearchType(String resourceName, String id, String url,
				TokenValueAndSearchType identifier, ReferenceSearchType type)
		{
			this.resourceName = resourceName;
			this.id = id;
			this.url = url;
			this.type = type;
			this.identifier = identifier;
		}

		// [parameter]=[uuid] -> local id
		//
		// [parameter]=[Type]/[uuid] -> local type+id
		//
		// [parameter]=[url] -> absolute id or canonical
		//
		// [parameter]:identifier=[identifier] -> identifier (not supported for canonical references)
		// [parameter]:[Type]=[uuid] -> local type+id
		public static ReferenceValueAndSearchType fromParamValue(List<? super SearchQueryParameterError> errors,
				List<String> targetResourceTypeNames, String parameterName, String queryParameterName,
				String queryParameterValue)
		{
			// simple case
			if (parameterName.equals(queryParameterName))
			{
				if (queryParameterValue.indexOf('/') == -1 && targetResourceTypeNames.size() == 1)
					return new ReferenceValueAndSearchType(targetResourceTypeNames.get(0), queryParameterValue, null,
							null, ReferenceSearchType.ID);
				else if (queryParameterValue.indexOf('/') == -1 && targetResourceTypeNames.size() > 1)
					return new ReferenceValueAndSearchType(null, queryParameterValue, null, null,
							ReferenceSearchType.ID);
				else if (queryParameterValue.startsWith("http"))
					return new ReferenceValueAndSearchType(null, null, queryParameterValue, null,
							ReferenceSearchType.URL);
				else if (queryParameterValue.indexOf('/') >= 0)
				{
					String[] splitAtSlash = queryParameterValue.split("/");
					if (splitAtSlash.length == 2)
					{

						if (targetResourceTypeNames.stream().anyMatch(name -> name.equals(splitAtSlash[0])))
							return new ReferenceValueAndSearchType(splitAtSlash[0], splitAtSlash[1], null, null,
									ReferenceSearchType.RESOURCE_NAME_AND_ID);
						else
						{
							errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE,
									parameterName, queryParameterValue, "Unsupported target resource type name "
											+ splitAtSlash[0] + ", not one of " + targetResourceTypeNames));
							return null;
						}
					}
					else
					{
						errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE,
								parameterName, queryParameterValue,
								"Unsupported reference " + queryParameterValue + " not 'type/id'"));
						return null;
					}
				}
				else
				{
					errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE,
							parameterName, queryParameterValue));
					return null;
				}
			}

			// identifier
			// parameter:identifier=value
			// parameter:identifier=system|value
			else if ((parameterName + PARAMETER_NAME_IDENTIFIER_MODIFIER).equals(queryParameterName))
			{
				TokenValueAndSearchType identifier = TokenValueAndSearchType.fromParamValue(parameterName,
						queryParameterName, queryParameterValue);
				return new ReferenceValueAndSearchType(null, null, null, identifier, ReferenceSearchType.IDENTIFIER);
			}

			// typed parameter
			// parameter:type=id
			// parameter:type=type/id
			else
			{
				Optional<String> type = targetResourceTypeNames.stream()
						.filter(t -> (parameterName + ":" + t).equals(queryParameterName)).findFirst();

				if (type.isPresent())
				{
					if (queryParameterValue.indexOf('/') == -1)
					{
						return new ReferenceValueAndSearchType(type.get(), queryParameterValue, null, null,
								ReferenceSearchType.TYPE_AND_ID);
					}
					else
					{
						String[] splitAtSlash = queryParameterValue.split("/");

						if (splitAtSlash.length == 2)
						{
							if (type.get().equals(splitAtSlash[0]))
							{
								return new ReferenceValueAndSearchType(type.get(), splitAtSlash[1], null, null,
										ReferenceSearchType.TYPE_AND_RESOURCE_NAME_AND_ID);
							}
							else
							{
								errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE,
										parameterName, queryParameterValue, "Inconsistent target resource type name "
												+ type.get() + " vs. " + splitAtSlash[0]));
								return null;
							}
						}
						else
						{
							errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE,
									parameterName, queryParameterValue,
									"Unsupported reference " + queryParameterValue + " not 'type/id'"));
							return null;
						}
					}
				}
				else
				{
					errors.add(new SearchQueryParameterError(SearchQueryParameterErrorType.UNPARSABLE_VALUE,
							parameterName, queryParameterValue, "Unsupported target resource type in "
									+ queryParameterName + " not one of " + targetResourceTypeNames));
					return null;
				}
			}
		}
	}

	private final Class<R> resourceType;
	private final List<String> targetResourceTypeNames;

	protected ReferenceValueAndSearchType valueAndType;

	public AbstractReferenceParameter(Class<R> resourceType, String parameterName, String... targetResourceTypeNames)
	{
		super(parameterName);

		this.resourceType = resourceType;
		this.targetResourceTypeNames = Arrays.asList(targetResourceTypeNames);
	}

	@Override
	protected void doConfigure(List<? super SearchQueryParameterError> errors, String queryParameterName,
			String queryParameterValue)
	{
		valueAndType = ReferenceValueAndSearchType.fromParamValue(errors, targetResourceTypeNames, parameterName,
				queryParameterName, queryParameterValue);
	}

	@Override
	public boolean isDefined()
	{
		return valueAndType != null;
	}

	@Override
	public String getBundleUriQueryParameterName()
	{
		return switch (valueAndType.type)
		{
			case ID, URL, RESOURCE_NAME_AND_ID -> parameterName;
			case TYPE_AND_ID, TYPE_AND_RESOURCE_NAME_AND_ID -> parameterName + ":" + valueAndType.resourceName;
			case IDENTIFIER -> parameterName + PARAMETER_NAME_IDENTIFIER_MODIFIER;
			default -> throw new IllegalArgumentException(
					"Unexpected " + ReferenceSearchType.class.getName() + " value: " + valueAndType.type);
		};
	}

	@Override
	public String getBundleUriQueryParameterValue()
	{
		return switch (valueAndType.type)
		{
			case ID, TYPE_AND_ID -> valueAndType.id;
			case URL -> valueAndType.url;
			case RESOURCE_NAME_AND_ID, TYPE_AND_RESOURCE_NAME_AND_ID ->
				valueAndType.resourceName + "/" + valueAndType.id;
			case IDENTIFIER -> switch (valueAndType.identifier.type)
			{
				case CODE -> valueAndType.identifier.codeValue;
				case CODE_AND_SYSTEM -> valueAndType.identifier.systemValue + "|" + valueAndType.identifier.codeValue;
				case CODE_AND_NO_SYSTEM_PROPERTY -> "|" + valueAndType.identifier.codeValue;
				case SYSTEM -> valueAndType.identifier.systemValue + "|";
				default -> throw new IllegalArgumentException(
						"Unexpected " + TokenSearchType.class.getName() + " value: " + valueAndType.identifier.type);
			};
			default -> throw new IllegalArgumentException(
					"Unexpected " + ReferenceSearchType.class.getName() + " value: " + valueAndType.type);
		};
	}

	@Override
	public void resolveReferencesForMatching(Resource resource, DaoProvider daoProvider) throws SQLException
	{
		if (resourceType.isInstance(resource))
			doResolveReferencesForMatching(resourceType.cast(resource), daoProvider);
	}

	protected abstract void doResolveReferencesForMatching(R resource, DaoProvider daoProvider) throws SQLException;

	@Override
	public SearchQueryIncludeParameterConfiguration configureInclude(List<? super SearchQueryParameterError> errors,
			String queryParameterIncludeValue)
	{
		IncludeParts includeParts = IncludeParts.fromString(queryParameterIncludeValue);
		String includeSql = getIncludeSql(includeParts);

		if (includeSql != null)
			return new SearchQueryIncludeParameterConfiguration(includeSql, includeParts,
					(resource, connection) -> modifyIncludeResource(includeParts, resource, connection));
		else
			return null;
	}

	protected abstract String getIncludeSql(IncludeParts includeParts);

	/**
	 * Use this method to modify the include resources. This method can be used if the resources returned by the include
	 * SQL are not complete and additional content needs to be retrieved from a not included column. For example the
	 * content of a {@link Binary} resource might not be stored in the json column.
	 *
	 * @param includeParts
	 *            not <code>null</code>
	 * @param resource
	 *            not <code>null</code>
	 * @param connection
	 *            not <code>null</code>
	 */
	protected abstract void modifyIncludeResource(IncludeParts includeParts, Resource resource, Connection connection);
}
