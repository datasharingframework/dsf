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
package dev.dsf.bpe.plugin;

import java.util.Objects;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;

public class ProcessStateChangeOutcome
{
	private final ProcessIdAndVersion processKeyAndVersion;
	private final ProcessState oldProcessState;
	private final ProcessState newProcessState;

	public ProcessStateChangeOutcome(ProcessIdAndVersion processKeyAndVersion, ProcessState oldProcessState,
			ProcessState newProcessState)
	{
		this.processKeyAndVersion = Objects.requireNonNull(processKeyAndVersion, "processKeyAndVersion");
		this.oldProcessState = Objects.requireNonNull(oldProcessState, "oldProcessState");
		this.newProcessState = Objects.requireNonNull(newProcessState, "newProcessState");
	}

	public ProcessIdAndVersion getProcessKeyAndVersion()
	{
		return processKeyAndVersion;
	}

	public ProcessState getOldProcessState()
	{
		return oldProcessState;
	}

	public ProcessState getNewProcessState()
	{
		return newProcessState;
	}

	@Override
	public String toString()
	{
		return getProcessKeyAndVersion().toString() + " " + getOldProcessState() + " -> " + getNewProcessState();
	}
}
