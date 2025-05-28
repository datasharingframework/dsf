package dev.dsf.fhir.dao;

import java.sql.SQLException;
import java.util.Optional;

import org.hl7.fhir.r4.model.Endpoint;

public interface EndpointDao extends ResourceDao<Endpoint>
{
	boolean existsActiveNotDeletedByAddress(String address) throws SQLException;

	Optional<Endpoint> readActiveNotDeletedByAddress(String address) throws SQLException;

	Optional<Endpoint> readActiveNotDeletedByThumbprint(String thumbprintHex) throws SQLException;
}
