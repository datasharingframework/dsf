package dev.dsf.bpe.api.plugin;

import java.nio.file.Path;

import org.operaton.bpm.model.bpmn.BpmnModelInstance;

public final record BpmnFileAndModel(int processPluginApiVersion, boolean draft, String file, BpmnModelInstance model,
		Path jar)
{
	public ProcessIdAndVersion toProcessIdAndVersion()
	{
		return ProcessIdAndVersion.fromModel(model());
	}
}