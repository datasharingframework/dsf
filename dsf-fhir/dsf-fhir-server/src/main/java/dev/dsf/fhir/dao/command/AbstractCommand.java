/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
