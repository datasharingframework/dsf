package dev.dsf.fhir.dao.jdbc;

import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

import org.hl7.fhir.r4.model.Resource;
import org.postgresql.util.PGobject;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;

abstract class AbstractPreparedStatementFactory<R extends Resource> implements PreparedStatementFactory<R>
{
	private final FhirContext fhirContext;
	private final Class<R> resourceType;

	private final String createSql;
	private final String readByIdSql;
	private final String readByIdAndVersionSql;
	private final String updateSql;

	protected AbstractPreparedStatementFactory(FhirContext fhirContext, Class<R> resourceType, String createSql,
			String readByIdSql, String readByIdAndVersionSql, String updateSql)
	{
		this.fhirContext = Objects.requireNonNull(fhirContext, "fhirContext");
		this.resourceType = Objects.requireNonNull(resourceType, "resourceType");
		this.createSql = Objects.requireNonNull(createSql, "createSql");
		this.readByIdSql = Objects.requireNonNull(readByIdSql, "readByIdSql");
		this.readByIdAndVersionSql = Objects.requireNonNull(readByIdAndVersionSql, "readByIdAndVersionSql");
		this.updateSql = Objects.requireNonNull(updateSql, "updateSql");
	}

	@Override
	public IParser getJsonParser()
	{
		IParser p = fhirContext.newJsonParser();
		p.setStripVersionsFromReferences(false);
		return p;
	}

	protected final R jsonToResource(String json)
	{
		return getJsonParser().parseResource(resourceType, json);
	}

	@Override
	public final PGobject resourceToPgObject(R resource)
	{
		if (resource == null)
			return null;

		try
		{
			PGobject o = new PGobject();
			o.setType("JSONB");
			o.setValue(getJsonParser().encodeResourceToString(resource));
			return o;
		}
		catch (DataFormatException | SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	@Override
	public final PGobject uuidToPgObject(UUID uuid)
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
	public final String getCreateSql()
	{
		return createSql;
	}

	@Override
	public final String getReadByIdSql()
	{
		return readByIdSql;
	}

	@Override
	public final String getReadByIdAndVersionSql()
	{
		return readByIdAndVersionSql;
	}

	@Override
	public final String getUpdateSql()
	{
		return updateSql;
	}
}
