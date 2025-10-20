package dev.dsf.bpe.v2.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Type;

import dev.dsf.bpe.v2.client.dsf.DsfClient;

public class StartTaskUpdaterImpl implements StartTaskUpdater
{
	private final DsfClient client;

	private final Supplier<Task> getStartTask;
	private final Consumer<Task> updateTask;

	public StartTaskUpdaterImpl(DsfClient client, Supplier<Task> getStartTask, Consumer<Task> updateTask)
	{
		this.client = Objects.requireNonNull(client, "client");

		this.getStartTask = Objects.requireNonNull(getStartTask, "getStartTask");
		this.updateTask = Objects.requireNonNull(updateTask, "updateTask");
	}

	@Override
	public void addOutput(Coding outputType, Type outputValue)
	{
		Task task = getStartTask.get();
		task.addOutput().setValue(outputValue).getType().addCoding(outputType);

		Task updated = client.update(task);
		updateTask.accept(updated);
	}

	@Override
	public Optional<TaskOutputComponent> getOutput(Coding outputType)
	{
		checkOutputType(outputType);

		Task task = getStartTask.get();
		return doGetOutput(task, outputType);
	}

	private Optional<TaskOutputComponent> doGetOutput(Task task, Coding outputType)
	{
		return task.getOutput().stream().filter(matchesSystemAndCodeOptionallyVersion(outputType)).findFirst();
	}

	private Predicate<TaskOutputComponent> matchesSystemAndCodeOptionallyVersion(Coding outputType)
	{
		return o -> o.getType().getCoding().stream()
				.anyMatch(c -> Objects.equals(c.getSystem(), outputType.getSystem())
						&& Objects.equals(c.getCode(), outputType.getCode()) && outputType.hasVersion()
								? Objects.equals(c.getVersion(), outputType.getVersion())
								: true);
	}

	@Override
	public void modifyOutput(Coding outputType, Type outputValue)
	{
		checkOutputType(outputType);

		Task task = getStartTask.get();

		doGetOutput(task, outputType)
				.orElseThrow(() -> new IllegalArgumentException("Output for type " + outputType.getSystem() + "|"
						+ outputType.getCode()
						+ (outputType.hasVersion() ? " (version: " + outputType.getVersion() + ") not found" : "")))
				.setValue(outputValue);

		Task updated = client.update(task);
		updateTask.accept(updated);
	}

	@Override
	public void removeOutput(Coding outputType)
	{
		checkOutputType(outputType);

		Task task = getStartTask.get();

		List<TaskOutputComponent> filtered = task.getOutput().stream()
				.filter(matchesSystemAndCodeOptionallyVersion(outputType).negate()).toList();

		if (task.getOutput().size() == filtered.size())
			throw new IllegalArgumentException("Output for type " + outputType.getSystem() + "|" + outputType.getCode()
					+ (outputType.hasVersion() ? " (version: " + outputType.getVersion() + ") not found" : ""));

		task.setOutput(filtered);

		Task updated = client.update(task);
		updateTask.accept(updated);
	}

	private void checkOutputType(Coding outputType)
	{
		Objects.requireNonNull(outputType, "outputType");

		Objects.requireNonNull(outputType.getSystem(), "outputType.system");
		Objects.requireNonNull(outputType.getCode(), "outputType.code");
		Objects.requireNonNull(outputType.getVersion(), "outputType.version");

		if (outputType.getSystem().isBlank())
			throw new IllegalArgumentException("outputType.system is blank");
		if (outputType.getCode().isBlank())
			throw new IllegalArgumentException("outputType.code is blank");
		if (outputType.getVersion().isBlank())
			throw new IllegalArgumentException("outputType.version is blank");
	}
}
