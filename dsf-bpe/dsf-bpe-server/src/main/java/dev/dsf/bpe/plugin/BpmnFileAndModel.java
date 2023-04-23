package dev.dsf.bpe.plugin;

import java.nio.file.Path;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

public final class BpmnFileAndModel
{
	private final boolean draft;
	private final String file;
	private final BpmnModelInstance model;
	private final Path jar;

	public BpmnFileAndModel(boolean draft, String file, BpmnModelInstance model, Path jar)
	{
		this.draft = draft;
		this.file = file;
		this.model = model;
		this.jar = jar;
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