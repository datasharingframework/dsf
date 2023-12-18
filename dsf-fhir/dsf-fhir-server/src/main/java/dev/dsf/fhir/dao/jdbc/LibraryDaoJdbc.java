package dev.dsf.fhir.dao.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Library;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.LibraryDao;
import dev.dsf.fhir.search.filter.LibraryIdentityFilter;
import dev.dsf.fhir.search.parameters.LibraryDate;
import dev.dsf.fhir.search.parameters.LibraryIdentifier;
import dev.dsf.fhir.search.parameters.LibraryName;
import dev.dsf.fhir.search.parameters.LibraryStatus;
import dev.dsf.fhir.search.parameters.LibraryUrl;
import dev.dsf.fhir.search.parameters.LibraryVersion;
import dev.dsf.fhir.search.parameters.LocationIdentifier;

public class LibraryDaoJdbc extends AbstractResourceDaoJdbc<Library> implements LibraryDao
{
	private final ReadByUrlDaoJdbc<Library> readByUrl;

	public LibraryDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Library.class, "libraries", "library", "library_id",
				LibraryIdentityFilter::new,
				Arrays.asList(factory(LibraryDate.PARAMETER_NAME, LibraryDate::new),
						factory(LibraryIdentifier.PARAMETER_NAME, LibraryIdentifier::new,
								LocationIdentifier.getNameModifiers()),
						factory(LibraryName.PARAMETER_NAME, LibraryName::new, LibraryName.getNameModifiers()),
						factory(LibraryStatus.PARAMETER_NAME, LibraryStatus::new, LibraryStatus.getNameModifiers()),
						factory(LibraryUrl.PARAMETER_NAME, LibraryUrl::new, LibraryUrl.getNameModifiers()),
						factory(LibraryVersion.PARAMETER_NAME, LibraryVersion::new, LibraryVersion.getNameModifiers())),
				Collections.emptyList());

		readByUrl = new ReadByUrlDaoJdbc<>(this::getDataSource, this::getResource, getResourceTable(),
				getResourceColumn());
	}

	@Override
	protected Library copy(Library resource)
	{
		return resource.copy();
	}

	@Override
	public Optional<Library> readByUrlAndVersion(String urlAndVersion) throws SQLException
	{
		return readByUrl.readByUrlAndVersion(urlAndVersion);
	}

	@Override
	public Optional<Library> readByUrlAndVersionWithTransaction(Connection connection, String urlAndVersion)
			throws SQLException
	{
		return readByUrl.readByUrlAndVersionWithTransaction(connection, urlAndVersion);
	}

	@Override
	public Optional<Library> readByUrlAndVersion(String url, String version) throws SQLException
	{
		return readByUrl.readByUrlAndVersion(url, version);
	}

	@Override
	public Optional<Library> readByUrlAndVersionWithTransaction(Connection connection, String url, String version)
			throws SQLException
	{
		return readByUrl.readByUrlAndVersionWithTransaction(connection, url, version);
	}
}
