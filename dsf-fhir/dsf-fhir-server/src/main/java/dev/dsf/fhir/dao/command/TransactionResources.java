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

import dev.dsf.fhir.validation.SnapshotGenerator;

public class TransactionResources
{
	private final ValidationHelper validationHelper;
	private final SnapshotGenerator snapshotGenerator;
	private final TransactionEventHandler transactionEventHandler;

	public TransactionResources(ValidationHelper validationHelper, SnapshotGenerator snapshotGenerator,
			TransactionEventHandler transactionEventHandler)
	{
		this.validationHelper = validationHelper;
		this.snapshotGenerator = snapshotGenerator;
		this.transactionEventHandler = transactionEventHandler;
	}

	public ValidationHelper getValidationHelper()
	{
		return validationHelper;
	}

	public SnapshotGenerator getSnapshotGenerator()
	{
		return snapshotGenerator;
	}

	public TransactionEventHandler getTransactionEventHandler()
	{
		return transactionEventHandler;
	}
}
