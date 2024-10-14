package dev.dsf.bpe.plugin;

import java.util.List;

import dev.dsf.bpe.api.plugin.BpmnFileAndModel;

public interface BpmnProcessStateChangeService
{
	/**
	 * @param models
	 *            models to deploy, not <code>null</code>
	 * @return list of state changes
	 */
	List<ProcessStateChangeOutcome> deploySuspendOrActivateProcesses(List<BpmnFileAndModel> models);
}
