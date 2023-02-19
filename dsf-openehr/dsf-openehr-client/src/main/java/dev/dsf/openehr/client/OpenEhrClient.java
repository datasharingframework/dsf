package dev.dsf.openehr.client;

import dev.dsf.openehr.model.structure.ResultSet;
import jakarta.ws.rs.core.MultivaluedMap;

public interface OpenEhrClient
{
	ResultSet query(String query, MultivaluedMap<String, Object> headers);
}
