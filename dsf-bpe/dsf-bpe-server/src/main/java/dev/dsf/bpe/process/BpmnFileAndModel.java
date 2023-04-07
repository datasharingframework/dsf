package dev.dsf.bpe.process;

import java.nio.file.Path;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;

public final class BpmnFileAndModel
{
	private final String file;
	private final BpmnModelInstance model;
	private final Path jar;

	public BpmnFileAndModel(String file, BpmnModelInstance model, Path jar)
	{
		this.file = file;
		this.model = model;
		this.jar = jar;
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

	public ProcessKeyAndVersion getProcessKeyAndVersion()
	{
		return ProcessKeyAndVersion.fromModel(getModel());
	}
}