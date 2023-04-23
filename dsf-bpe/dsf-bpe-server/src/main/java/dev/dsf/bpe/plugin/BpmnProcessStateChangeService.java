package dev.dsf.bpe.plugin;

import java.util.List;

public interface BpmnProcessStateChangeService
{
	/**
	 * @param models
	 *            models to deploy, not <code>null</code>
	 * @return list of state changes
	 */
	List<ProcessStateChangeOutcome> deploySuspendOrActivateProcesses(List<BpmnFileAndModel> models);
}
