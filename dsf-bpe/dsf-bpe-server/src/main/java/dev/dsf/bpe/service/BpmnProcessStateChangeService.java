package dev.dsf.bpe.service;

import java.util.List;
import java.util.stream.Stream;

import dev.dsf.bpe.process.BpmnFileAndModel;
import dev.dsf.bpe.process.ProcessStateChangeOutcome;

public interface BpmnProcessStateChangeService
{
	/**
	 * @param models
	 *            models to deploy, not <code>null</code>
	 * @return list of state changes
	 */
	List<ProcessStateChangeOutcome> deploySuspendOrActivateProcesses(Stream<BpmnFileAndModel> models);
}
