package dev.dsf.fhir.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.hl7.fhir.r4.model.Resource;

import dev.dsf.fhir.history.AtParameter;
import dev.dsf.fhir.history.History;
import dev.dsf.fhir.history.SinceParameter;
import dev.dsf.fhir.history.filter.HistoryIdentityFilter;
import dev.dsf.fhir.search.PageAndCount;

public interface HistoryDao
{
	History readHistory(List<HistoryIdentityFilter> filters, PageAndCount pageAndCount, AtParameter atParameter,
			SinceParameter sinceParameter) throws SQLException;

	History readHistory(HistoryIdentityFilter filter, PageAndCount pageAndCount, AtParameter atParameter,
			SinceParameter sinceParameter, Class<? extends Resource> resource) throws SQLException;

	History readHistory(HistoryIdentityFilter filter, PageAndCount pageAndCount, AtParameter atParameter,
			SinceParameter sinceParameter, Class<? extends Resource> resource, UUID id) throws SQLException;
}
