package dev.dsf.fhir.webservice.secure;

import java.util.Objects;
import java.util.Optional;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleType;
import org.hl7.fhir.r4.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.fhir.authorization.AuthorizationRule;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.webservice.specification.RootService;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

public class RootServiceSecure extends AbstractServiceSecure<RootService> implements RootService
{
	private static final Logger logger = LoggerFactory.getLogger(AbstractResourceServiceSecure.class);

	private final AuthorizationRule<Resource> authorizationRule;

	public RootServiceSecure(RootService delegate, String serverBase, ResponseGenerator responseGenerator,
			ReferenceResolver referenceResolver, AuthorizationRule<Resource> authorizationRule)
	{
		super(delegate, serverBase, responseGenerator, referenceResolver);

		this.authorizationRule = authorizationRule;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		super.afterPropertiesSet();

		Objects.requireNonNull(authorizationRule, "authorizationRule");
	}

	@Override
	public Response root(UriInfo uri, HttpHeaders headers)
	{
		logCurrentIdentity();

		// get root allowed for all authenticated users

		return delegate.root(uri, headers);
	}

	@Override
	public Response handleBundle(Bundle bundle, UriInfo uri, HttpHeaders headers)
	{
		logCurrentIdentity();

		Optional<String> reasonHandleBundleAllowed = reasonHandleBundleAllowed(bundle);

		if (reasonHandleBundleAllowed.isEmpty())
		{
			audit.info("Handling of transaction and batch bundles denied for identity '{}'",
					getCurrentIdentity().getName());
			return forbidden("bundle");
		}
		else
		{
			audit.info("Handling of transaction or batch bundle allowed for identity '{}': {}",
					getCurrentIdentity().getName(), reasonHandleBundleAllowed.get());
			return delegate.handleBundle(bundle, uri, headers);
		}
	}

	private Optional<String> reasonHandleBundleAllowed(Bundle bundle)
	{
		if (BundleType.BATCH.equals(bundle.getType()) || BundleType.TRANSACTION.equals(bundle.getType()))
		{
			logger.info(
					"Handling of batch or transaction bundles generaly allowed for all, entries will be individualy evaluated");
			return Optional.of("Allowed for all, entries individualy evaluated");
		}
		else
		{
			logger.warn("Handling bundle denied, not a batch or transaction bundle");
			return Optional.empty();
		}
	}

	@Override
	public Response history(UriInfo uri, HttpHeaders headers)
	{
		logCurrentIdentity();

		Optional<String> reasonHistoryAllowed = authorizationRule.reasonHistoryAllowed(getCurrentIdentity());
		if (reasonHistoryAllowed.isEmpty())
		{
			audit.info("Root History denied for user '{}'", getCurrentIdentity().getName());
			return forbidden("search");
		}
		else
		{
			audit.info("Root History allowed for user '{}': {}", getCurrentIdentity().getName(),
					reasonHistoryAllowed.get());
			return delegate.history(uri, headers);
		}
	}
}
