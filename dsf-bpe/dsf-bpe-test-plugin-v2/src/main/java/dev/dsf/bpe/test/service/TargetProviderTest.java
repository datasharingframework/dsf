package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectException;
import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Identifier;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.constants.CodeSystems.OrganizationRole;
import dev.dsf.bpe.v2.constants.NamingSystems.OrganizationIdentifier;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.service.TargetProvider;
import dev.dsf.bpe.v2.variables.Target;
import dev.dsf.bpe.v2.variables.Targets;
import dev.dsf.bpe.v2.variables.Variables;

public class TargetProviderTest extends AbstractTest implements ServiceTask
{
	private static final String ORGANIZATION_IDENTIFIER_PARENT_VALUE = "Parent_Organization";
	private static final Identifier ORGANIZATION_IDENTIFIER_PARENT = OrganizationIdentifier
			.withValue(ORGANIZATION_IDENTIFIER_PARENT_VALUE);

	private static final String ORGANIZATION_IDENTIFIER_LOCAL_VALUE = "Test_Organization";
	private static final Identifier ORGANIZATION_IDENTIFIER_LOCAL = OrganizationIdentifier
			.withValue(ORGANIZATION_IDENTIFIER_LOCAL_VALUE);
	private static final String ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE = "External_Test_Organization";
	private static final Identifier ORGANIZATION_IDENTIFIER_EXTERNAL = OrganizationIdentifier
			.withValue(ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE);

	private static final String ENDPOINT_IDENTIFIER_LOCAL_VALUE = "Test_Endpoint";
	private static final String ENDPOINT_IDENTIFIER_EXTERNAL_VALUE = "External_Test_Endpoint";

	private static final String ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE = "not-existing-identifier-value";
	private static final Identifier ORGANIZATION_IDENTIFIER_NOT_EXISTING = OrganizationIdentifier
			.withValue(ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE);
	private static final String MEMBER_ROLE_NOT_EXISTING_CODE = "not-existing-role";
	private static final Coding MEMBER_ROLE_NOT_EXISTING = OrganizationRole.withCode(MEMBER_ROLE_NOT_EXISTING_CODE);

	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		executeTests(api, variables, api.getTargetProvider());
	}

	@PluginTest
	public void createForExistingParentWithCorrelationKey(TargetProvider targetProvider) throws Exception
	{
		Targets targets = targetProvider.create(ORGANIZATION_IDENTIFIER_PARENT_VALUE).withCorrelationKey();
		expectNotNull(targets);
		expectSame(2, targets.size());

		List<Target> entries = targets.getEntries().stream()
				.sorted(Comparator.comparing(Target::getOrganizationIdentifierValue).reversed()).toList();
		expectNotNull(entries);
		expectSame(2, entries.size());

		Target target0 = entries.get(0);
		expectNotNull(target0);
		expectSame(ORGANIZATION_IDENTIFIER_LOCAL_VALUE, target0.getOrganizationIdentifierValue());
		expectSame(ENDPOINT_IDENTIFIER_LOCAL_VALUE, target0.getEndpointIdentifierValue());
		expectNotNull(target0.getEndpointUrl());
		expectNotNull(target0.getCorrelationKey());

		Target target1 = entries.get(1);
		expectNotNull(target1);
		expectSame(ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE, target1.getOrganizationIdentifierValue());
		expectSame(ENDPOINT_IDENTIFIER_EXTERNAL_VALUE, target1.getEndpointIdentifierValue());
		expectNotNull(target1.getEndpointUrl());
		expectNotNull(target1.getCorrelationKey());
	}

	@PluginTest
	public void createForExistingParentWithoutCorrelationKey(TargetProvider targetProvider) throws Exception
	{
		Targets targets = targetProvider.create(ORGANIZATION_IDENTIFIER_PARENT_VALUE).withoutCorrelationKey();
		expectNotNull(targets);
		expectSame(2, targets.size());

		List<Target> entries = targets.getEntries().stream()
				.sorted(Comparator.comparing(Target::getOrganizationIdentifierValue).reversed()).toList();
		expectNotNull(entries);
		expectSame(2, entries.size());

		Target target0 = entries.get(0);
		expectNotNull(target0);
		expectSame(ORGANIZATION_IDENTIFIER_LOCAL_VALUE, target0.getOrganizationIdentifierValue());
		expectSame(ENDPOINT_IDENTIFIER_LOCAL_VALUE, target0.getEndpointIdentifierValue());
		expectNotNull(target0.getEndpointUrl());
		expectNull(target0.getCorrelationKey());

		Target target1 = entries.get(1);
		expectNotNull(target1);
		expectSame(ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE, target1.getOrganizationIdentifierValue());
		expectSame(ENDPOINT_IDENTIFIER_EXTERNAL_VALUE, target1.getEndpointIdentifierValue());
		expectNotNull(target1.getEndpointUrl());
		expectNull(target1.getCorrelationKey());
	}

	@PluginTest
	public void createForExistingParentWithFilter(TargetProvider targetProvider) throws Exception
	{
		AtomicInteger counter = new AtomicInteger(0);

		Targets targets = targetProvider.create(ORGANIZATION_IDENTIFIER_PARENT_VALUE).filter((a, o, e) ->
		{
			expectNotNull(a);
			expectNotNull(o);
			expectNotNull(e);

			counter.incrementAndGet();

			return false;

		}).withCorrelationKey();

		expectNotNull(targets);
		expectSame(0, targets.size());

		List<Target> entries = targets.getEntries().stream()
				.sorted(Comparator.comparing(Target::getOrganizationIdentifierValue).reversed()).toList();
		expectNotNull(entries);
		expectSame(0, entries.size());

		expectSame(2, counter.get());
	}

	@PluginTest
	public void createForNotExistingParentIdentifierValue(TargetProvider targetProvider) throws Exception
	{
		Targets targets = targetProvider.create(ORGANIZATION_IDENTIFIER_NOT_EXISTING_VALUE).withCorrelationKey();
		expectNotNull(targets);
		expectSame(0, targets.size());

		List<Target> entries = targets.getEntries().stream()
				.sorted(Comparator.comparing(Target::getOrganizationIdentifierValue).reversed()).toList();
		expectNotNull(entries);
		expectSame(0, entries.size());
	}

	@PluginTest
	public void createForNotExistingParentIdentifier(TargetProvider targetProvider) throws Exception
	{
		Targets targets = targetProvider.create(ORGANIZATION_IDENTIFIER_NOT_EXISTING).withCorrelationKey();
		expectNotNull(targets);
		expectSame(0, targets.size());

		List<Target> entries = targets.getEntries().stream()
				.sorted(Comparator.comparing(Target::getOrganizationIdentifierValue).reversed()).toList();
		expectNotNull(entries);
		expectSame(0, entries.size());
	}

	@PluginTest
	public void createForExistingParentWithCorrelationKeyDic(TargetProvider targetProvider) throws Exception
	{
		Targets targets = targetProvider.create(ORGANIZATION_IDENTIFIER_PARENT, OrganizationRole.dic())
				.withCorrelationKey();
		expectNotNull(targets);
		expectSame(1, targets.size());

		List<Target> entries = targets.getEntries().stream()
				.sorted(Comparator.comparing(Target::getOrganizationIdentifierValue).reversed()).toList();
		expectNotNull(entries);
		expectSame(1, entries.size());

		Target target0 = entries.get(0);
		expectNotNull(target0);
		expectSame(ORGANIZATION_IDENTIFIER_LOCAL_VALUE, target0.getOrganizationIdentifierValue());
		expectSame(ENDPOINT_IDENTIFIER_LOCAL_VALUE, target0.getEndpointIdentifierValue());
		expectNotNull(target0.getEndpointUrl());
		expectNotNull(target0.getCorrelationKey());
	}

	@PluginTest
	public void createForExistingParentWithCorrelationKeyDts(TargetProvider targetProvider) throws Exception
	{
		Targets targets = targetProvider.create(ORGANIZATION_IDENTIFIER_PARENT, OrganizationRole.dts())
				.withCorrelationKey();
		expectNotNull(targets);
		expectSame(1, targets.size());

		List<Target> entries = targets.getEntries().stream()
				.sorted(Comparator.comparing(Target::getOrganizationIdentifierValue).reversed()).toList();
		expectNotNull(entries);
		expectSame(1, entries.size());

		Target target0 = entries.get(0);
		expectNotNull(target0);
		expectSame(ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE, target0.getOrganizationIdentifierValue());
		expectSame(ENDPOINT_IDENTIFIER_EXTERNAL_VALUE, target0.getEndpointIdentifierValue());
		expectNotNull(target0.getEndpointUrl());
		expectNotNull(target0.getCorrelationKey());
	}

	@PluginTest
	public void createForExistingParentWithCorrelationKeyDicMemberIdentifier(TargetProvider targetProvider)
			throws Exception
	{
		Targets targets = targetProvider
				.create(ORGANIZATION_IDENTIFIER_PARENT, OrganizationRole.dic(), ORGANIZATION_IDENTIFIER_LOCAL)
				.withCorrelationKey();
		expectNotNull(targets);
		expectSame(1, targets.size());

		List<Target> entries = targets.getEntries().stream()
				.sorted(Comparator.comparing(Target::getOrganizationIdentifierValue).reversed()).toList();
		expectNotNull(entries);
		expectSame(1, entries.size());

		Target target0 = entries.get(0);
		expectNotNull(target0);
		expectSame(ORGANIZATION_IDENTIFIER_LOCAL_VALUE, target0.getOrganizationIdentifierValue());
		expectSame(ENDPOINT_IDENTIFIER_LOCAL_VALUE, target0.getEndpointIdentifierValue());
		expectNotNull(target0.getEndpointUrl());
		expectNotNull(target0.getCorrelationKey());
	}

	@PluginTest
	public void createForExistingParentWithCorrelationKeyDicMemberIdentifierWithoutDicRole(
			TargetProvider targetProvider) throws Exception
	{
		Targets targets = targetProvider
				.create(ORGANIZATION_IDENTIFIER_PARENT, OrganizationRole.dic(), ORGANIZATION_IDENTIFIER_EXTERNAL)
				.withCorrelationKey();
		expectNotNull(targets);
		expectSame(0, targets.size());

		List<Target> entries = targets.getEntries().stream()
				.sorted(Comparator.comparing(Target::getOrganizationIdentifierValue).reversed()).toList();
		expectNotNull(entries);
		expectSame(0, entries.size());
	}

	@PluginTest
	public void createNull(TargetProvider targetProvider) throws Exception
	{
		expectException(NullPointerException.class, () -> targetProvider.create((String) null).withoutCorrelationKey());
		expectException(NullPointerException.class,
				() -> targetProvider.create((Identifier) null).withoutCorrelationKey());
	}

	@PluginTest
	public void createNotNullNull(TargetProvider targetProvider) throws Exception
	{
		expectException(NullPointerException.class, () -> targetProvider
				.create(ORGANIZATION_IDENTIFIER_PARENT_VALUE, (String) null).withoutCorrelationKey());
		expectException(NullPointerException.class,
				() -> targetProvider.create(ORGANIZATION_IDENTIFIER_PARENT, (Coding) null).withoutCorrelationKey());
	}

	@PluginTest
	public void createNotNullNotNullNull(TargetProvider targetProvider) throws Exception
	{
		expectException(NullPointerException.class,
				() -> targetProvider
						.create(ORGANIZATION_IDENTIFIER_PARENT_VALUE, MEMBER_ROLE_NOT_EXISTING_CODE, (String[]) null)
						.withoutCorrelationKey());
		expectException(NullPointerException.class,
				() -> targetProvider
						.create(ORGANIZATION_IDENTIFIER_PARENT, MEMBER_ROLE_NOT_EXISTING, (Identifier[]) null)
						.withoutCorrelationKey());
	}

	@PluginTest
	public void createForExistingParentWithCorrelationKeyNullFilter(TargetProvider targetProvider) throws Exception
	{
		Targets targets = targetProvider.create(ORGANIZATION_IDENTIFIER_PARENT_VALUE).filter(null).withCorrelationKey();
		expectNotNull(targets);
		expectSame(2, targets.size());

		List<Target> entries = targets.getEntries().stream()
				.sorted(Comparator.comparing(Target::getOrganizationIdentifierValue).reversed()).toList();
		expectNotNull(entries);
		expectSame(2, entries.size());

		Target target0 = entries.get(0);
		expectNotNull(target0);
		expectSame(ORGANIZATION_IDENTIFIER_LOCAL_VALUE, target0.getOrganizationIdentifierValue());
		expectSame(ENDPOINT_IDENTIFIER_LOCAL_VALUE, target0.getEndpointIdentifierValue());
		expectNotNull(target0.getEndpointUrl());
		expectNotNull(target0.getCorrelationKey());

		Target target1 = entries.get(1);
		expectNotNull(target1);
		expectSame(ORGANIZATION_IDENTIFIER_EXTERNAL_VALUE, target1.getOrganizationIdentifierValue());
		expectSame(ENDPOINT_IDENTIFIER_EXTERNAL_VALUE, target1.getEndpointIdentifierValue());
		expectNotNull(target1.getEndpointUrl());
		expectNotNull(target1.getCorrelationKey());
	}
}
