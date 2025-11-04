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
package dev.dsf.bpe.v2.activity;

import java.util.Objects;
import java.util.function.Function;

import org.operaton.bpm.engine.delegate.DelegateExecution;

import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.variables.Variables;

public abstract class AbstractProcessPluginDelegate<D>
{
	protected final ProcessPluginApi api;
	protected final D delegate;

	private final Function<DelegateExecution, Variables> variablesFactory;

	public AbstractProcessPluginDelegate(ProcessPluginApi api, Function<DelegateExecution, Variables> variablesFactory,
			D delegate)
	{
		this.api = Objects.requireNonNull(api, "api");
		this.variablesFactory = Objects.requireNonNull(variablesFactory, "variablesFactory");
		this.delegate = Objects.requireNonNull(delegate, "delegate");
	}

	protected Variables createVariables(DelegateExecution execution)
	{
		return variablesFactory.apply(execution);
	}
}
