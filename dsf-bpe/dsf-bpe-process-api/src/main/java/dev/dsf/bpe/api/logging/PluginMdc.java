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
package dev.dsf.bpe.api.logging;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.DelegateTask;

public interface PluginMdc
{
	@FunctionalInterface
	public interface ConsumerWithException<T>
	{
		void accept(T t) throws Exception;
	}

	void executeWithProcessMdc(DelegateTask delegateTask, Consumer<DelegateTask> executable);

	void executeWithProcessMdc(DelegateExecution delegateExecution, ConsumerWithException<DelegateExecution> executable)
			throws Exception;

	void executeWithPluginMdc(Runnable runnable);

	boolean executeWithPluginMdc(Supplier<Boolean> supplier);
}