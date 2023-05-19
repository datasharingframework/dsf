package dev.dsf.fhir.dao.command;

import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.common.auth.conf.Identity;
import dev.dsf.fhir.prefer.PreferReturnType;

public abstract class AbstractCommand implements Command
{
	protected static final Logger audit = LoggerFactory.getLogger("dsf-audit-logger");

	private final int transactionPriority;

	protected final int index;

	protected final Identity identity;
	protected final PreferReturnType returnType;
	protected final Bundle bundle;
	protected final BundleEntryComponent entry;

	protected final String serverBase;

	protected final AuthorizationHelper authorizationHelper;

	public AbstractCommand(int transactionPriority, int index, Identity identity, PreferReturnType returnType,
			Bundle bundle, BundleEntryComponent entry, String serverBase, AuthorizationHelper authorizationHelper)
	{
		this.transactionPriority = transactionPriority;

		this.index = index;

		this.identity = identity;
		this.returnType = returnType;
		this.bundle = bundle;
		this.entry = entry;
		this.serverBase = serverBase;

		this.authorizationHelper = authorizationHelper;
	}

	@Override
	public final int getIndex()
	{
		return index;
	}

	@Override
	public final int getTransactionPriority()
	{
		return transactionPriority;
	}

	@Override
	public Identity getIdentity()
	{
		return identity;
	}
}
