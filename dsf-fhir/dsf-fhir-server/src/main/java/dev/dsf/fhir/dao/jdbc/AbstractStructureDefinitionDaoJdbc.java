package dev.dsf.fhir.dao.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.dao.StructureDefinitionDao;
import dev.dsf.fhir.search.SearchQueryIdentityFilter;
import dev.dsf.fhir.search.SearchQueryParameter;
import dev.dsf.fhir.search.SearchQueryParameterFactory;
import dev.dsf.fhir.search.parameters.StructureDefinitionDate;
import dev.dsf.fhir.search.parameters.StructureDefinitionIdentifier;
import dev.dsf.fhir.search.parameters.StructureDefinitionStatus;
import dev.dsf.fhir.search.parameters.StructureDefinitionUrl;
import dev.dsf.fhir.search.parameters.StructureDefinitionVersion;

abstract class AbstractStructureDefinitionDaoJdbc extends AbstractResourceDaoJdbc<StructureDefinition>
		implements StructureDefinitionDao
{
	private static <R extends Resource> SearchQueryParameterFactory<R> factory(String resourceColumn,
			String parameterName, Function<String, SearchQueryParameter<R>> supplier, List<String> nameModifiers)
	{
		return factory(parameterName, () -> supplier.apply(resourceColumn), nameModifiers);
	}

	private static <R extends Resource> SearchQueryParameterFactory<R> factory(String resourceColumn,
			String parameterName, Function<String, SearchQueryParameter<R>> supplier)
	{
		return factory(parameterName, () -> supplier.apply(resourceColumn));
	}

	private final ReadByUrlDaoJdbc<StructureDefinition> readByUrl;

	public AbstractStructureDefinitionDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource,
			FhirContext fhirContext, String resourceTable, String resourceColumn, String resourceIdColumn,
			Function<Identity, SearchQueryIdentityFilter> userFilter)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, StructureDefinition.class, resourceTable,
				resourceColumn, resourceIdColumn, userFilter,
				Arrays.asList(
						factory(resourceColumn, StructureDefinitionDate.PARAMETER_NAME, StructureDefinitionDate::new),
						factory(resourceColumn, StructureDefinitionIdentifier.PARAMETER_NAME,
								StructureDefinitionIdentifier::new, StructureDefinitionIdentifier.getNameModifiers()),
						factory(resourceColumn, StructureDefinitionStatus.PARAMETER_NAME,
								StructureDefinitionStatus::new, StructureDefinitionStatus.getNameModifiers()),
						factory(resourceColumn, StructureDefinitionUrl.PARAMETER_NAME, StructureDefinitionUrl::new,
								StructureDefinitionUrl.getNameModifiers()),
						factory(resourceColumn, StructureDefinitionVersion.PARAMETER_NAME,
								StructureDefinitionVersion::new, StructureDefinitionVersion.getNameModifiers())),
				Collections.emptyList());

		readByUrl = new ReadByUrlDaoJdbc<StructureDefinition>(this::getDataSource, this::getResource, resourceTable,
				resourceColumn);
	}

	@Override
	public Optional<StructureDefinition> readByUrlAndVersion(String urlAndVersion) throws SQLException
	{
		return readByUrl.readByUrlAndVersion(urlAndVersion);
	}

	@Override
	public Optional<StructureDefinition> readByUrlAndVersionWithTransaction(Connection connection, String urlAndVersion)
			throws SQLException
	{
		return readByUrl.readByUrlAndVersionWithTransaction(connection, urlAndVersion);
	}

	@Override
	public Optional<StructureDefinition> readByUrlAndVersion(String url, String version) throws SQLException
	{
		return readByUrl.readByUrlAndVersion(url, version);
	}

	@Override
	public Optional<StructureDefinition> readByUrlAndVersionWithTransaction(Connection connection, String url,
			String version) throws SQLException
	{
		return readByUrl.readByUrlAndVersionWithTransaction(connection, url, version);
	}
}
