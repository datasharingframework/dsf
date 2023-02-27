package dev.dsf.openehr.client;

import javax.ws.rs.core.MultivaluedMap;

import dev.dsf.openehr.model.structure.ResultSet;

public interface OpenEhrClient
{
	ResultSet query(String query, MultivaluedMap<String, Object> headers);
}
