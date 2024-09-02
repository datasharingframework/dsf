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
