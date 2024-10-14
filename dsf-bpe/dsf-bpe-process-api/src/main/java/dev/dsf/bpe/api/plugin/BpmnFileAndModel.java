package dev.dsf.bpe.api.plugin;

import java.nio.file.Path;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

public final class BpmnFileAndModel
{
	private final int processPluginApiVersion;
	private final boolean draft;
	private final String file;
	private final BpmnModelInstance model;
	private final Path jar;

	public BpmnFileAndModel(int processPluginApiVersion, boolean draft, String file, BpmnModelInstance model, Path jar)
	{
		this.processPluginApiVersion = processPluginApiVersion;
		this.draft = draft;
		this.file = file;
		this.model = model;
		this.jar = jar;
	}

	public int getProcessPluginApiVersion()
	{
		return processPluginApiVersion;
	}

	public boolean isDraft()
	{
		return draft;
	}

	public String getFile()
	{
		return file;
	}

	public BpmnModelInstance getModel()
	{
		return model;
	}

	public Path getJar()
	{
		return jar;
	}

	public ProcessIdAndVersion getProcessIdAndVersion()
	{
		return ProcessIdAndVersion.fromModel(getModel());
	}
}