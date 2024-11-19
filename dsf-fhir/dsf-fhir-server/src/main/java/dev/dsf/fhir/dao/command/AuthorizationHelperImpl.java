package dev.dsf.fhir.dao.command;

import java.sql.Connection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.SearchEntryMode;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.authorization.AuthorizationRuleProvider;
import dev.dsf.fhir.help.ResponseGenerator;
import jakarta.ws.rs.WebApplicationException;

public class AuthorizationHelperImpl implements AuthorizationHelper
{
	private static final Logger logger = LoggerFactory.getLogger(AuthorizationHelperImpl.class);
	private static final Logger audit = LoggerFactory.getLogger("dsf-audit-logger");

	private final AuthorizationRuleProvider authorizationRuleProvider;
	private final ResponseGenerator responseGenerator;

	public AuthorizationHelperImpl(AuthorizationRuleProvider authorizationRuleProvider,
			ResponseGenerator responseGenerator)
	{
		this.authorizationRuleProvider = authorizationRuleProvider;
		this.responseGenerator = responseGenerator;
	}

	@SuppressWarnings("unchecked")
	private Optional<AuthorizationRule<Resource>> getAuthorizationRule(Class<?> resourceClass)
	{
		return authorizationRuleProvider.getAuthorizationRule(resourceClass)
				.map(rule -> (AuthorizationRule<Resource>) rule);
	}

	@SuppressWarnings("unchecked")
	private Optional<AuthorizationRule<Resource>> getAuthorizationRule(String resourceTypeName)
	{
		return authorizationRuleProvider.getAuthorizationRule(resourceTypeName)
				.map(rule -> (AuthorizationRule<Resource>) rule);
	}

	private WebApplicationException forbidden(String operation, Identity identity) throws WebApplicationException
	{
		return new WebApplicationException(responseGenerator.forbiddenNotAllowed(operation, identity));
	}

	@Override
	public void checkCreateAllowed(int index, Connection connection, Identity identity, Resource newResource)
			throws WebApplicationException
	{
		final String resourceTypeName = getResourceTypeName(newResource);

		Optional<AuthorizationRule<Resource>> optRule = getAuthorizationRule(newResource.getClass());
		optRule.flatMap(rule -> rule.reasonCreateAllowed(connection, identity, newResource)).ifPresentOrElse(reason ->
		{
			audit.info("Create of {} allowed for identity '{}' via bundle at index {}, reason: {}", resourceTypeName,
					identity.getName(), index, reason);
		}, () ->
		{
			audit.info("Create of {} denied for identity '{}' via bundle at index {}", resourceTypeName,
					identity.getName(), index);
			throw forbidden("create", identity);
		});
	}

	private String getResourceTypeName(Resource resource)
	{
		return resource.getResourceType().name();
	}

	@Override
	public void checkReadAllowed(int index, Connection connection, Identity identity, Resource existingResource)
			throws WebApplicationException
	{
		final String resourceTypeName = getResourceTypeName(existingResource);
		final String resourceId = existingResource.getIdElement().getIdPart();
		final long resourceVersion = existingResource.getIdElement().getVersionIdPartAsLong();

		Optional<AuthorizationRule<Resource>> optRule = getAuthorizationRule(existingResource.getClass());
		optRule.flatMap(rule -> rule.reasonReadAllowed(connection, identity, existingResource))
				.ifPresentOrElse(reason ->
				{
					audit.info("Read of {}/{}/_history/{} allowed for identity '{}' via bundle at index {}, reason: {}",
							resourceTypeName, resourceId, resourceVersion, identity.getName(), index, reason);
				}, () ->
				{
					audit.info("Read of {}/{}/_history/{} denied for identity '{}' via bundle at index {}",
							resourceTypeName, resourceId, resourceVersion, identity.getName(), index);
					throw forbidden("read", identity);
				});
	}

	@Override
	public void checkUpdateAllowed(int index, Connection connection, Identity identity, Resource oldResource,
			Resource newResource) throws WebApplicationException
	{
		final String resourceTypeName = getResourceTypeName(oldResource);
		final String resourceId = oldResource.getIdElement().getIdPart();
		final long resourceVersion = oldResource.getIdElement().getVersionIdPartAsLong();

		Optional<AuthorizationRule<Resource>> optRule = getAuthorizationRule(oldResource.getClass());
		optRule.flatMap(rule -> rule.reasonUpdateAllowed(connection, identity, oldResource, newResource))
				.ifPresentOrElse(reason ->
				{
					audit.info(
							"Update of {}/{}/_history/{} allowed for identity '{}' via bundle at index {}, reason: {}",
							resourceTypeName, resourceId, resourceVersion, identity.getName(), index, reason);
				}, () ->
				{
					audit.info("Update of {}/{}/_history/{} denied for identity '{}' via bundle at index {}",
							resourceTypeName, resourceId, resourceVersion, identity.getName(), index);
					throw forbidden("update", identity);
				});
	}

	@Override
	public void checkDeleteAllowed(int index, Connection connection, Identity identity, Resource oldResource)
			throws WebApplicationException
	{
		final String resourceTypeName = getResourceTypeName(oldResource);
		final String resourceId = oldResource.getIdElement().getIdPart();
		final long resourceVersion = oldResource.getIdElement().getVersionIdPartAsLong();

		Optional<AuthorizationRule<Resource>> optRule = getAuthorizationRule(oldResource.getClass());
		optRule.flatMap(rule -> rule.reasonDeleteAllowed(identity, oldResource)).ifPresentOrElse(reason ->
		{
			audit.info("Delete of {}/{}/_history/{} allowed for identity '{}' via bundle at index {}, reason: {}",
					resourceTypeName, resourceId, resourceVersion, identity.getName(), index, reason);
		}, () ->
		{
			audit.info("Delete of {}/{}/_history/{} denied for identity '{}' via bundle at index {}", resourceTypeName,
					resourceId, resourceVersion, identity.getName(), index);
			throw forbidden("delete", identity);
		});
	}

	@Override
	public void checkSearchAllowed(int index, Identity identity, String resourceTypeName) throws WebApplicationException
	{
		Optional<AuthorizationRule<Resource>> optRule = getAuthorizationRule(resourceTypeName);
		optRule.flatMap(rule -> rule.reasonSearchAllowed(identity)).ifPresentOrElse(reason ->
		{
			audit.info("Search of {} allowed for identity '{}' via bundle at index {}, reason: {}", resourceTypeName,
					identity.getName(), index, reason);
		}, () ->
		{
			audit.info("Search of {} denied for identity '{}' via bundle at index {}", resourceTypeName,
					identity.getName(), index);
			throw forbidden("search", identity);
		});
	}

	@Override
	public void filterIncludeResults(int index, Connection connection, Identity identity, Bundle multipleResult)
	{
		List<BundleEntryComponent> filteredEntries = multipleResult.getEntry().stream()
				.filter(c -> SearchEntryMode.MATCH.equals(c.getSearch().getMode())
						|| (SearchEntryMode.INCLUDE.equals(c.getSearch().getMode())
								&& filterIncludeResource(index, identity, c.getResource())))
				.collect(Collectors.toList());
		multipleResult.setEntry(filteredEntries);
	}

	private boolean filterIncludeResource(int index, Identity identity, Resource include)
	{
		final String resourceTypeName = getResourceTypeName(include);
		final String resourceId = include.getIdElement().getIdPart();
		final long resourceVersion = include.getIdElement().getVersionIdPartAsLong();

		Optional<AuthorizationRule<Resource>> optRule = getAuthorizationRule(include.getClass());
		return optRule.flatMap(rule -> rule.reasonReadAllowed(identity, include)).map(reason ->
		{
			logger.debug("Inclusion of {}/{}/_history/{} allowed for identity '{}' via bundle at index {}: {}",
					resourceTypeName, resourceId, resourceVersion, identity.getName(), index, reason);
			return true;
		}).orElseGet(() ->
		{
			logger.debug(
					"Inclusion of {}/{}/_history/{} denied for identity '{} via bundle at index {}: read not allowed",
					resourceTypeName, resourceId, resourceVersion, identity.getName(), index);
			return false;
		});
	}
}
