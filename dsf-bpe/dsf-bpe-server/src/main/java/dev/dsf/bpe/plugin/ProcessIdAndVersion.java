package dev.dsf.bpe.plugin;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.Process;

public class ProcessIdAndVersion implements Comparable<ProcessIdAndVersion>
{
	public static ProcessIdAndVersion fromString(String idAndVersion)
	{
		Objects.requireNonNull(idAndVersion, "idAndVersion");

		String[] split = idAndVersion.split("\\|");
		if (split.length != 2)
			throw new IllegalArgumentException("Format: 'id|version' expected");

		return new ProcessIdAndVersion(split[0], split[1]);
	}

	public static List<ProcessIdAndVersion> fromStrings(List<String> idAndVersions)
	{
		Objects.requireNonNull(idAndVersions, "idAndVersions");

		return idAndVersions.stream().filter(s -> s != null && !s.isBlank()).map(ProcessIdAndVersion::fromString)
				.collect(Collectors.toList());
	}

	public static ProcessIdAndVersion fromDefinition(ProcessDefinition definition)
	{
		Objects.requireNonNull(definition, "definition");

		return new ProcessIdAndVersion(definition.getKey(), definition.getVersionTag());
	}

	public static ProcessIdAndVersion fromModel(BpmnModelInstance model)
	{
		Objects.requireNonNull(model, "model");

		Process process = model.getModelElementsByType(Process.class).stream().findFirst().get();
		return new ProcessIdAndVersion(process.getId(), process.getCamundaVersionTag());
	}

	private final String id;
	private final String version;

	public ProcessIdAndVersion(String id, String version)
	{
		this.id = id;
		this.version = version;
	}

	public String getId()
	{
		return id;
	}

	public String getVersion()
	{
		return version;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProcessIdAndVersion other = (ProcessIdAndVersion) obj;
		if (id == null)
		{
			if (other.id != null)
				return false;
		}
		else if (!id.equals(other.id))
			return false;
		if (version == null)
		{
			if (other.version != null)
				return false;
		}
		else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return getId() + "|" + getVersion();
	}

	@Override
	public int compareTo(ProcessIdAndVersion o)
	{
		return Comparator.comparing(ProcessIdAndVersion::getId).thenComparing(ProcessIdAndVersion::getVersion)
				.compare(this, o);
	}
}