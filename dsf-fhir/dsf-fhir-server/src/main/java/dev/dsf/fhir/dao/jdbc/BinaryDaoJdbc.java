package dev.dsf.fhir.dao.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.IdType;
import org.postgresql.util.PGobject;

import ca.uhn.fhir.context.FhirContext;
import dev.dsf.fhir.dao.BinaryDao;
import dev.dsf.fhir.search.filter.BinaryIdentityFilter;
import dev.dsf.fhir.search.parameters.BinaryContentType;

public class BinaryDaoJdbc extends AbstractResourceDaoJdbc<Binary> implements BinaryDao
{
	public BinaryDaoJdbc(DataSource dataSource, DataSource permanentDeleteDataSource, FhirContext fhirContext)
	{
		super(dataSource, permanentDeleteDataSource, Binary.class, "binaries", "binary_json", "binary_id",
				new PreparedStatementFactoryBinary(fhirContext), BinaryIdentityFilter::new,
				Arrays.asList(factory(BinaryContentType.PARAMETER_NAME, BinaryContentType::new,
						BinaryContentType.getNameModifiers())),
				Collections.emptyList());
	}

	@Override
	protected Binary copy(Binary resource)
	{
		return resource.copy();
	}

	@Override
	protected Binary getResource(ResultSet result, int index) throws SQLException
	{
		// TODO Bugfix HAPI is removing version information from binary.id
		Binary binary = super.getResource(result, index);
		IdType fixedId = new IdType(binary.getResourceType().name(), binary.getIdElement().getIdPart(),
				binary.getMeta().getVersionId());
		binary.setIdElement(fixedId);
		return binary;
	}

	@Override
	protected void modifySearchResultResource(Binary resource, Connection connection) throws SQLException
	{
		try (PreparedStatement statement = connection
				.prepareStatement("SELECT binary_data FROM binaries WHERE binary_id = ? AND version = ?"))
		{
			PGobject uuidObject = getPreparedStatementFactory()
					.uuidToPgObject(toUuid(resource.getIdElement().getIdPart()));
			Long version = resource.getMeta().getVersionIdElement().getIdPartAsLong();

			statement.setObject(1, uuidObject);
			statement.setLong(2, version);

			try (ResultSet result = statement.executeQuery())
			{
				if (result.next())
				{
					byte[] data = result.getBytes(1);
					resource.setData(data);
				}
				else
					throw new SQLException(
							"Binary resource with id " + resource.getIdElement().getIdPart() + " not found");
			}
		}
	}
}
