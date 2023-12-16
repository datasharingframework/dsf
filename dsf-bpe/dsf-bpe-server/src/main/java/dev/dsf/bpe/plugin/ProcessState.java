package dev.dsf.bpe.plugin;

public enum ProcessState
{
	MISSING(-1), NEW(-1), ACTIVE(3), DRAFT(2), RETIRED(1), EXCLUDED(0);

	private final int priority;

	ProcessState(int priority)
	{
		this.priority = priority;
	}

	public boolean isHigherPriority(ProcessState then)
	{
		return priority > then.priority;
	}
}