package dev.dsf.fhir.dao.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Measure;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.MeasureDao;
import dev.dsf.fhir.search.filter.MeasureIdentityFilter;
import dev.dsf.fhir.search.parameters.MeasureDate;
import dev.dsf.fhir.search.parameters.MeasureDependsOn;
import dev.dsf.fhir.search.parameters.MeasureIdentifier;
import dev.dsf.fhir.search.parameters.MeasureName;
import dev.dsf.fhir.search.parameters.MeasureStatus;
import dev.dsf.fhir.search.parameters.MeasureUrl;
import dev.dsf.fhir.search.parameters.MeasureVersion;

public class MeasureDaoJdbc extends AbstractResourceDaoJdbc<Measure> implements MeasureDao
{
	private final ReadByUrlDaoJdbc<Measure> readByUrl;

	public MeasureDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, fhirContext, Measure.class, "measures", "measure", "measure_id",
				MeasureIdentityFilter::new,
				List.of(factory(MeasureDate.PARAMETER_NAME, MeasureDate::new),
						factory(MeasureDependsOn.PARAMETER_NAME, MeasureDependsOn::new,
								MeasureDependsOn.getNameModifiers(), MeasureDependsOn::new,
								MeasureDependsOn.getIncludeParameterValues()),
						factory(MeasureIdentifier.PARAMETER_NAME, MeasureIdentifier::new,
								MeasureIdentifier.getNameModifiers()),
						factory(MeasureName.PARAMETER_NAME, MeasureName::new, MeasureName.getNameModifiers()),
						factory(MeasureStatus.PARAMETER_NAME, MeasureStatus::new, MeasureStatus.getNameModifiers()),
						factory(MeasureUrl.PARAMETER_NAME, MeasureUrl::new, MeasureUrl.getNameModifiers()),
						factory(MeasureVersion.PARAMETER_NAME, MeasureVersion::new, MeasureVersion.getNameModifiers())),
				List.of());

		readByUrl = new ReadByUrlDaoJdbc<>(this::getDataSource, this::getResource, getResourceTable(),
				getResourceColumn());
	}

	@Override
	protected Measure copy(Measure resource)
	{
		return resource.copy();
	}

	@Override
	public Optional<Measure> readByUrlAndVersion(String urlAndVersion) throws SQLException
	{
		return readByUrl.readByUrlAndVersion(urlAndVersion);
	}

	@Override
	public Optional<Measure> readByUrlAndVersionWithTransaction(Connection connection, String urlAndVersion)
			throws SQLException
	{
		return readByUrl.readByUrlAndVersionWithTransaction(connection, urlAndVersion);
	}

	@Override
	public Optional<Measure> readByUrlAndVersion(String url, String version) throws SQLException
	{
		return readByUrl.readByUrlAndVersion(url, version);
	}

	@Override
	public Optional<Measure> readByUrlAndVersionWithTransaction(Connection connection, String url, String version)
			throws SQLException
	{
		return readByUrl.readByUrlAndVersionWithTransaction(connection, url, version);
	}
}
