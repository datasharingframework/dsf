package dev.dsf.fhir.history;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.InitializingBean;

import ca.uhn.fhir.model.api.annotation.ResourceDef;
import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.dao.HistoryDao;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.help.SummaryMode;
import dev.dsf.fhir.history.filter.HistoryIdentityFilterFactory;
import dev.dsf.fhir.prefer.PreferHandlingType;
import dev.dsf.fhir.search.PageAndCount;
import dev.dsf.fhir.search.SearchQuery;
import dev.dsf.fhir.search.SearchQueryParameterError;
import dev.dsf.fhir.service.ReferenceCleaner;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

public class HistoryServiceImpl implements HistoryService, InitializingBean
{
	private final String serverBase;
	private final int defaultPageCount;
	private final ParameterConverter parameterConverter;
	private final ExceptionHandler exceptionHandler;
	private final ResponseGenerator responseGenerator;
	private final ReferenceCleaner referenceCleaner;
	private final HistoryDao historyDao;
	private final HistoryIdentityFilterFactory historyUserFilterFactory;

	public HistoryServiceImpl(String serverBase, int defaultPageCount, ParameterConverter parameterConverter,
			ExceptionHandler exceptionHandler, ResponseGenerator responseGenerator, ReferenceCleaner referenceCleaner,
			HistoryDao historyDao, HistoryIdentityFilterFactory historyUserFilterFactory)
	{
		this.serverBase = serverBase;
		this.defaultPageCount = defaultPageCount;
		this.parameterConverter = parameterConverter;
		this.exceptionHandler = exceptionHandler;
		this.responseGenerator = responseGenerator;
		this.referenceCleaner = referenceCleaner;
		this.historyDao = historyDao;
		this.historyUserFilterFactory = historyUserFilterFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(serverBase, "serverBase");
		Objects.requireNonNull(parameterConverter, "parameterConverter");
		Objects.requireNonNull(exceptionHandler, "exceptionHandler");
		Objects.requireNonNull(responseGenerator, "responseGenerator");
		Objects.requireNonNull(referenceCleaner, "referenceCleaner");
		Objects.requireNonNull(historyDao, "historyDao");
		Objects.requireNonNull(historyUserFilterFactory, "historyUserFilterFactory");
	}

	@Override
	public Bundle getHistory(Identity identity, UriInfo uri, HttpHeaders headers)
	{
		return getHistory(identity, uri, headers, null, null);
	}

	@Override
	public Bundle getHistory(Identity identity, UriInfo uri, HttpHeaders headers, Class<? extends Resource> resource)
	{
		return getHistory(identity, uri, headers, resource, null);
	}

	@Override
	public Bundle getHistory(Identity identity, UriInfo uri, HttpHeaders headers, Class<? extends Resource> resource,
			String id)
	{
		MultivaluedMap<String, String> queryParameters = uri.getQueryParameters();

		Integer page = parameterConverter.getFirstInt(queryParameters, SearchQuery.PARAMETER_PAGE);
		int effectivePage = page == null ? 1 : page;

		Integer count = parameterConverter.getFirstInt(queryParameters, SearchQuery.PARAMETER_COUNT);
		int effectiveCount = (count == null || count < 0) ? defaultPageCount : count;

		PageAndCount pageAndCount = new PageAndCount(effectivePage, effectiveCount);

		AtParameter atParameter = new AtParameter();
		atParameter.configure(queryParameters);
		SinceParameter sinceParameter = new SinceParameter();
		sinceParameter.configure(queryParameters);

		String path = null;
		History history;
		if (resource == null && id == null)
			history = exceptionHandler.handleSqlException(() -> historyDao.readHistory(
					historyUserFilterFactory.getIdentityFilters(identity), pageAndCount, atParameter, sinceParameter));
		else if (resource != null && id != null)
		{
			history = exceptionHandler.handleSqlException(() -> historyDao.readHistory(
					historyUserFilterFactory.getIdentityFilter(identity, resource), pageAndCount, atParameter,
					sinceParameter, resource, parameterConverter.toUuid(getResourceTypeName(resource), id)));
			path = resource.getAnnotation(ResourceDef.class).name();
		}
		else if (resource != null)
		{
			history = exceptionHandler.handleSqlException(
					() -> historyDao.readHistory(historyUserFilterFactory.getIdentityFilter(identity, resource),
							pageAndCount, atParameter, sinceParameter, resource));
			path = resource.getAnnotation(ResourceDef.class).name();
		}
		else
			throw new WebApplicationException();

		List<SearchQueryParameterError> errors = new ArrayList<>();
		errors.addAll(atParameter.getErrors());
		errors.addAll(sinceParameter.getErrors());

		if (!errors.isEmpty() && PreferHandlingType.STRICT.equals(parameterConverter.getPreferHandling(headers)))
			throw new WebApplicationException(
					responseGenerator.response(Status.BAD_REQUEST, responseGenerator.toOperationOutcomeError(errors),
							parameterConverter.getMediaTypeThrowIfNotSupported(uri, headers)).build());

		String format = queryParameters.getFirst(SearchQuery.PARAMETER_FORMAT);
		String pretty = queryParameters.getFirst(SearchQuery.PARAMETER_PRETTY);
		SummaryMode summaryMode = SummaryMode.fromString(queryParameters.getFirst(SearchQuery.PARAMETER_SUMMARY));

		UriBuilder bundleUri = UriBuilder.fromPath(serverBase);
		if (path != null)
			bundleUri = bundleUri.path(path);
		if (path != null && id != null)
			bundleUri = bundleUri.path(id);

		bundleUri = bundleUri.path("_history");
		atParameter.modifyBundleUri(bundleUri);
		sinceParameter.modifyBundleUri(bundleUri);

		Bundle bundle = responseGenerator.createHistoryBundle(history, errors, bundleUri, format, pretty, summaryMode);
		// clean literal references from bundle entries
		bundle.getEntry().stream().filter(BundleEntryComponent::hasResource).map(BundleEntryComponent::getResource)
				.forEach(referenceCleaner::cleanLiteralReferences);
		return bundle;
	}

	private String getResourceTypeName(Class<? extends Resource> resource)
	{
		if (resource == null)
			return null;
		else
			return resource.getAnnotation(ResourceDef.class).name();
	}
}
