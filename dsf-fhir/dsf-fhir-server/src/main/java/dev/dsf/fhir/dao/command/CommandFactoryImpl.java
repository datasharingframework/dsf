package dev.dsf.fhir.dao.command;

import java.sql.Connection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.Bundle.HTTPVerb;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.springframework.beans.factory.InitializingBean;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.dao.ResourceDao;
import dev.dsf.fhir.dao.StructureDefinitionDao;
import dev.dsf.fhir.dao.exception.BadBundleException;
import dev.dsf.fhir.dao.provider.DaoProvider;
import dev.dsf.fhir.event.EventGenerator;
import dev.dsf.fhir.event.EventHandler;
import dev.dsf.fhir.help.ExceptionHandler;
import dev.dsf.fhir.help.ParameterConverter;
import dev.dsf.fhir.help.ResponseGenerator;
import dev.dsf.fhir.prefer.PreferHandlingType;
import dev.dsf.fhir.prefer.PreferReturnType;
import dev.dsf.fhir.service.ReferenceCleaner;
import dev.dsf.fhir.service.ReferenceExtractor;
import dev.dsf.fhir.service.ReferenceResolver;
import dev.dsf.fhir.validation.SnapshotGenerator;
import dev.dsf.fhir.validation.ValidationRules;

public class CommandFactoryImpl implements InitializingBean, CommandFactory
{
	private final String serverBase;
	private final int defaultPageCount;
	private final DataSource dataSource;
	private final DataSource permanentDeleteDataSource;
	private final String dbUsersGroup;
	private final DaoProvider daoProvider;
	private final ReferenceExtractor referenceExtractor;
	private final ReferenceResolver referenceResolver;
	private final ReferenceCleaner referenceCleaner;
	private final ResponseGenerator responseGenerator;
	private final ExceptionHandler exceptionHandler;
	private final ParameterConverter parameterConverter;
	private final EventHandler eventHandler;
	private final EventGenerator eventGenerator;
	private final AuthorizationHelper authorizationHelper;
	private final ValidationHelper validationHelper;
	private final SnapshotGenerator snapshotGenerator;
	private final ValidationRules validationRules;
	private final Function<Connection, TransactionResources> transactionResourcesFactory;

	public CommandFactoryImpl(String serverBase, int defaultPageCount, DataSource dataSource,
			DataSource permanentDeleteDataSource, String dbUsersGroup, DaoProvider daoProvider,
			ReferenceExtractor referenceExtractor, ReferenceResolver referenceResolver,
			ReferenceCleaner referenceCleaner, ResponseGenerator responseGenerator, ExceptionHandler exceptionHandler,
			ParameterConverter parameterConverter, EventHandler eventHandler, EventGenerator eventGenerator,
			AuthorizationHelper authorizationHelper, ValidationHelper validationHelper,
			SnapshotGenerator snapshotGenerator, ValidationRules validationRules,
			Function<Connection, TransactionResources> transactionResourcesFactory)
	{
		this.serverBase = serverBase;
		this.defaultPageCount = defaultPageCount;
		this.dataSource = dataSource;
		this.permanentDeleteDataSource = permanentDeleteDataSource;
		this.dbUsersGroup = dbUsersGroup;
		this.daoProvider = daoProvider;
		this.referenceExtractor = referenceExtractor;
		this.referenceResolver = referenceResolver;
		this.referenceCleaner = referenceCleaner;
		this.responseGenerator = responseGenerator;
		this.exceptionHandler = exceptionHandler;
		this.parameterConverter = parameterConverter;
		this.eventHandler = eventHandler;
		this.eventGenerator = eventGenerator;
		this.authorizationHelper = authorizationHelper;
		this.validationHelper = validationHelper;
		this.snapshotGenerator = snapshotGenerator;
		this.validationRules = validationRules;
		this.transactionResourcesFactory = transactionResourcesFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		Objects.requireNonNull(serverBase, "serverBase");
		Objects.requireNonNull(dataSource, "dataSource");
		Objects.requireNonNull(permanentDeleteDataSource, "permanentDeleteDataSource");

		Objects.requireNonNull(daoProvider, "daoProvider");
		Objects.requireNonNull(referenceExtractor, "referenceExtractor");
		Objects.requireNonNull(referenceResolver, "referenceResolver");
		Objects.requireNonNull(referenceCleaner, "referenceCleaner");
		Objects.requireNonNull(responseGenerator, "responseGenerator");
		Objects.requireNonNull(exceptionHandler, "exceptionHandler");
		Objects.requireNonNull(parameterConverter, "parameterConverter");
		Objects.requireNonNull(eventHandler, "eventHandler");
		Objects.requireNonNull(eventGenerator, "eventGenerator");
		Objects.requireNonNull(authorizationHelper, "authorizationHelper");
		Objects.requireNonNull(validationHelper, "validationHelper");
		Objects.requireNonNull(snapshotGenerator, "snapshotGenerator");
		Objects.requireNonNull(validationRules, "validationRules");
		Objects.requireNonNull(transactionResourcesFactory, "transactionResourcesFactory");
	}

	// head
	private Command head(int index, Identity identity, PreferReturnType returnType, Bundle bundle,
			BundleEntryComponent entry, PreferHandlingType handlingType)
	{
		return new HeadCommand(index, identity, returnType, bundle, entry, serverBase, authorizationHelper,
				defaultPageCount, daoProvider, parameterConverter, responseGenerator, exceptionHandler,
				referenceCleaner, handlingType);
	}

	// read, vread
	private Command get(int index, Identity identity, PreferReturnType returnType, Bundle bundle,
			BundleEntryComponent entry, PreferHandlingType handlingType)
	{
		return new ReadCommand(index, identity, returnType, bundle, entry, serverBase, authorizationHelper,
				defaultPageCount, daoProvider, parameterConverter, responseGenerator, exceptionHandler,
				referenceCleaner, handlingType);
	}

	// create, conditional create
	private <R extends Resource> Command post(int index, Identity identity, PreferReturnType returnType, Bundle bundle,
			BundleEntryComponent entry, R resource)
	{
		if (resource.getResourceType().name().equals(entry.getRequest().getUrl()))
		{
			@SuppressWarnings("unchecked")
			Optional<? extends ResourceDao<R>> dao = (Optional<? extends ResourceDao<R>>) daoProvider
					.getDao(resource.getClass());

			if (resource instanceof StructureDefinition s)
				return new CreateStructureDefinitionCommand(index, identity, returnType, bundle, entry, serverBase,
						authorizationHelper, s, (StructureDefinitionDao) dao.get(), exceptionHandler,
						parameterConverter, responseGenerator, referenceExtractor, referenceResolver, referenceCleaner,
						eventGenerator, daoProvider.getStructureDefinitionSnapshotDao());
			else
				return dao.map(d -> new CreateCommand<>(index, identity, returnType, bundle, entry, serverBase,
						authorizationHelper, resource, d, exceptionHandler, parameterConverter, responseGenerator,
						referenceExtractor, referenceResolver, referenceCleaner, eventGenerator))
						.orElseThrow(() -> new IllegalStateException(
								"Resource of type " + resource.getClass().getName() + " not supported"));
		}
		else
			throw new IllegalStateException(
					"Request url " + entry.getRequest().getUrl() + " for method POST not supported");
	}

	// update, conditional update
	private <R extends Resource> Command put(int index, Identity identity, PreferReturnType returnType, Bundle bundle,
			BundleEntryComponent entry, R resource)
	{
		if (entry.getRequest().getUrl() != null && !entry.getRequest().getUrl().isBlank()
				&& entry.getRequest().getUrl().startsWith(resource.getResourceType().name()))
		{
			@SuppressWarnings("unchecked")
			Optional<? extends ResourceDao<R>> dao = (Optional<? extends ResourceDao<R>>) daoProvider
					.getDao(resource.getClass());

			if (resource instanceof StructureDefinition s)
				return new UpdateStructureDefinitionCommand(index, identity, returnType, bundle, entry, serverBase,
						authorizationHelper, s, (StructureDefinitionDao) dao.get(), exceptionHandler,
						parameterConverter, responseGenerator, referenceExtractor, referenceResolver, referenceCleaner,
						eventGenerator, daoProvider.getStructureDefinitionSnapshotDao());
			else
				return dao.map(d -> new UpdateCommand<>(index, identity, returnType, bundle, entry, serverBase,
						authorizationHelper, resource, d, exceptionHandler, parameterConverter, responseGenerator,
						referenceExtractor, referenceResolver, referenceCleaner, eventGenerator))
						.orElseThrow(() -> new IllegalStateException(
								"Resource of type " + resource.getClass().getName() + " not supported"));
		}
		else
			throw new IllegalStateException(
					"Request url " + entry.getRequest().getUrl() + " for method POST not supported");
	}

	// delete, conditional delete
	private Command delete(int index, Identity identity, PreferReturnType returnType, Bundle bundle,
			BundleEntryComponent entry)
	{
		if (entry.getRequest().getUrl() != null && !entry.getRequest().getUrl().isBlank())
		{
			if (entry.getRequest().getUrl().startsWith("StructureDefinition"))
				return new DeleteStructureDefinitionCommand(index, identity, returnType, bundle, entry, serverBase,
						authorizationHelper, responseGenerator, daoProvider, exceptionHandler, parameterConverter,
						eventGenerator);
			else
				return new DeleteCommand(index, identity, returnType, bundle, entry, serverBase, authorizationHelper,
						responseGenerator, daoProvider, exceptionHandler, parameterConverter, eventGenerator);
		}
		else
			throw new BadBundleException(
					"Request url " + entry.getRequest().getUrl() + " for method DELETE not supported");
	}

	@Override
	public CommandList createCommands(Bundle bundle, Identity identity, PreferReturnType returnType,
			PreferHandlingType handlingType) throws BadBundleException
	{
		Objects.requireNonNull(bundle, "bundle");
		Objects.requireNonNull(identity, "identity");
		Objects.requireNonNull(returnType, "returnType");
		Objects.requireNonNull(handlingType, "handlingType");

		if (bundle.getType() != null)
		{
			List<Command> commands = IntStream
					.range(0, bundle.getEntry().size()).mapToObj(index -> createCommand(index, identity, returnType,
							handlingType, bundle, bundle.getEntry().get(index)))
					.flatMap(Function.identity()).collect(Collectors.toList());

			return switch (bundle.getType())
			{
				case BATCH ->
					new BatchCommandList(dataSource, permanentDeleteDataSource, dbUsersGroup, exceptionHandler,
							commands, validationHelper, snapshotGenerator, eventHandler, responseGenerator);

				case TRANSACTION -> new TransactionCommandList(dataSource, permanentDeleteDataSource, dbUsersGroup,
						exceptionHandler, commands, transactionResourcesFactory, responseGenerator);

				default -> throw new BadBundleException("Unsupported bundle type " + bundle.getType());
			};
		}
		else
			throw new BadBundleException("Missing bundle type");
	}

	protected Stream<Command> createCommand(int index, Identity identity, PreferReturnType returnType,
			PreferHandlingType handlingType, Bundle bundle, BundleEntryComponent entry)
	{
		if (entry.hasRequest() && entry.getRequest().hasMethod())
		{
			if (!entry.hasResource())
			{
				return switch (entry.getRequest().getMethod())
				{
					case GET -> Stream.of(get(index, identity, returnType, bundle, entry, handlingType));
					case HEAD -> Stream.of(head(index, identity, returnType, bundle, entry, handlingType));
					case DELETE -> Stream.of(delete(index, identity, returnType, bundle, entry));

					default -> throw new BadBundleException("Request method " + entry.getRequest().getMethod()
							+ " at index " + index + " not supported without resource");
				};
			}
			else
			{
				return switch (entry.getRequest().getMethod())
				{
					case POST ->
						resolveReferences(post(index, identity, returnType, bundle, entry, entry.getResource()), index,
								identity, returnType, bundle, entry, entry.getResource(), HTTPVerb.POST);

					case PUT -> resolveReferences(put(index, identity, returnType, bundle, entry, entry.getResource()),
							index, identity, returnType, bundle, entry, entry.getResource(), HTTPVerb.PUT);

					default -> throw new BadBundleException("Request method " + entry.getRequest().getMethod()
							+ " at index " + index + " not supported with resource");
				};
			}
		}
		else
			throw new BadBundleException("BundleEntry at index " + index + " has no request or request has no method");
	}

	private <R extends Resource> Stream<Command> resolveReferences(Command cmd, int index, Identity identity,
			PreferReturnType returnType, Bundle bundle, BundleEntryComponent entry, R resource, HTTPVerb verb)
	{
		@SuppressWarnings("unchecked")
		Optional<? extends ResourceDao<R>> dao = (Optional<? extends ResourceDao<R>>) daoProvider
				.getDao(resource.getClass());

		if (referenceExtractor.getReferences(resource).anyMatch(_ -> true)) // at least one entry
		{
			return dao.map(d -> Stream.of(cmd,
					new CheckReferencesCommand<R, ResourceDao<R>>(index, identity, returnType, bundle, entry,
							serverBase, authorizationHelper, resource, verb, d, exceptionHandler, parameterConverter,
							responseGenerator, referenceExtractor, referenceResolver, validationRules)))
					.orElseThrow(() -> new IllegalStateException(
							"Resource of type " + resource.getClass().getName() + " not supported"));
		}
		else
			return Stream.of(cmd);
	}
}
