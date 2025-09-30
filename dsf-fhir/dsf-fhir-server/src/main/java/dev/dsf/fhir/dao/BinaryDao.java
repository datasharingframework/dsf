package dev.dsf.fhir.dao;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.Binary;

import dev.dsf.fhir.dao.exception.ResourceDeletedException;
import dev.dsf.fhir.webservice.RangeRequest;

public interface BinaryDao extends ResourceDao<Binary>
{
	Optional<Binary> read(UUID uuid, RangeRequest rangeRequest) throws SQLException, ResourceDeletedException;

	Optional<Binary> readVersion(UUID uuid, long version, RangeRequest rangeRequest)
			throws SQLException, ResourceDeletedException;

	void executeLargeObjectUnlink();

	void stopLargeObjectUnlinker();
}
