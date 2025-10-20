package dev.dsf.bpe.v2.service;

import java.util.Objects;
import java.util.Optional;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Type;

import jakarta.ws.rs.WebApplicationException;

public interface StartTaskUpdater
{
	/**
	 * Adds an output parameter to the start task, updates the {@link Task} on the DSF FHIR server and updates the
	 * process variable.
	 *
	 * @param outputType
	 *            not <code>null</code>, must have system, code and version
	 * @param outputValue
	 *            may be <code>null</code>
	 * @throws WebApplicationException
	 *             if start task can not be update at the DSF FHIR server
	 * @throws IllegalArgumentException
	 *             if system, code or version of the given <b>outputType</b> is blank
	 */
	void addOutput(Coding outputType, Type outputValue) throws WebApplicationException;

	/**
	 * Adds an output parameter to the start task, updates the {@link Task} on the DSF FHIR server and updates the
	 * process variable.
	 *
	 * @param outputTypeSystem
	 *            not <code>null</code>, not blank
	 * @param outputTypeCode
	 *            not <code>null</code>, not blank
	 * @param outputTypeVersion
	 *            not <code>null</code>, not blank
	 * @param outputValue
	 *            may be <code>null</code>
	 * @throws WebApplicationException
	 *             if start task can not be update at the DSF FHIR server
	 * @throws IllegalArgumentException
	 *             if <b>outputTypeSystem</b>, <b>outputTypeCode</b> or <b>outputTypeVersion</b> is blank
	 */
	default void addOutput(String outputTypeSystem, String outputTypeCode, String outputTypeVersion, Type outputValue)
			throws WebApplicationException
	{
		Objects.requireNonNull(outputTypeSystem, "outputTypeSystem");
		Objects.requireNonNull(outputTypeCode, "outputTypeCode");
		Objects.requireNonNull(outputTypeVersion, "outputTypeVersion");

		if (outputTypeSystem.isBlank())
			throw new IllegalArgumentException("outputTypeSystem is blank");
		if (outputTypeCode.isBlank())
			throw new IllegalArgumentException("outputTypeCode is blank");
		if (outputTypeVersion.isBlank())
			throw new IllegalArgumentException("outputTypeVersion is blank");

		addOutput(new Coding(outputTypeSystem, outputTypeCode, null).setVersion(outputTypeVersion), outputValue);
	}

	/**
	 * @param outputType
	 *            not <code>null</code>, must have system and code and version
	 * @return Output with the given <b>outputType</b> from the start task if present
	 */
	Optional<TaskOutputComponent> getOutput(Coding outputType);

	/**
	 * @param outputTypeSystem
	 *            not <code>null</code>, not blank
	 * @param outputTypeCode
	 *            not <code>null</code>, not blank
	 * @param outputTypeVersion
	 *            not <code>null</code>, not blank
	 * @return Output with the given <b>outputType</b> from the start task if present
	 * @throws IllegalArgumentException
	 *             if <b>outputTypeSystem</b>, <b>outputTypeCode</b> or <b>outputTypeVersion</b> is blank
	 */
	default Optional<TaskOutputComponent> getOutput(String outputTypeSystem, String outputTypeCode,
			String outputTypeVersion)
	{
		Objects.requireNonNull(outputTypeSystem, "outputTypeSystem");
		Objects.requireNonNull(outputTypeCode, "outputTypeCode");
		Objects.requireNonNull(outputTypeVersion, "outputTypeVersion");

		if (outputTypeSystem.isBlank())
			throw new IllegalArgumentException("outputTypeSystem is blank");
		if (outputTypeCode.isBlank())
			throw new IllegalArgumentException("outputTypeCode is blank");
		if (outputTypeVersion.isBlank())
			throw new IllegalArgumentException("outputTypeVersion is blank");

		return getOutput(new Coding(outputTypeSystem, outputTypeCode, null).setVersion(outputTypeVersion));
	}

	/**
	 * @param outputType
	 *            not <code>null</code>, must have system and code and version
	 * @return <code>true</code> if the start task has output parameter with the given <b>outputType</b>
	 */
	default boolean hasOuput(Coding outputType)
	{
		return getOutput(outputType).isPresent();
	}

	/**
	 * Set the given <b>outputValue</b> for an output parameter of the start task with the given <b>outputType</b>,
	 * updates the {@link Task} on the DSF FHIR server and updates the process variable.
	 *
	 * @param outputTypeSystem
	 *            not <code>null</code>, not blank
	 * @param outputTypeCode
	 *            not <code>null</code>, not blank
	 * @param outputTypeVersion
	 *            not <code>null</code>, not blank
	 * @param outputValue
	 *            may be <code>null</code>
	 * @throws WebApplicationException
	 *             if start task can not be update at the DSF FHIR server
	 * @throws IllegalArgumentException
	 *             if the start task has no output parameter with the given outputType parameters or if
	 *             <b>outputTypeSystem</b>, <b>outputTypeCode</b> or <b>outputTypeVersion</b> is blank
	 */
	default void modifyOutput(String outputTypeSystem, String outputTypeCode, String outputTypeVersion,
			Type outputValue) throws WebApplicationException
	{
		Objects.requireNonNull(outputTypeSystem, "outputTypeSystem");
		Objects.requireNonNull(outputTypeCode, "outputTypeCode");
		Objects.requireNonNull(outputTypeVersion, "outputTypeVersion");

		if (outputTypeSystem.isBlank())
			throw new IllegalArgumentException("outputTypeSystem is blank");
		if (outputTypeCode.isBlank())
			throw new IllegalArgumentException("outputTypeCode is blank");
		if (outputTypeVersion.isBlank())
			throw new IllegalArgumentException("outputTypeVersion is blank");

		modifyOutput(new Coding(outputTypeSystem, outputTypeCode, null).setVersion(outputTypeVersion), outputValue);
	}

	/**
	 * Set the given <b>outputValue</b> for an output parameter of the start task with the given <b>outputType</b>,
	 * updates the {@link Task} on the DSF FHIR server and updates the process variable.
	 *
	 * @param outputType
	 *            not <code>null</code>, must have system, code and version
	 * @param outputValue
	 *            may be <code>null</code>
	 * @throws WebApplicationException
	 *             if start task can not be update at the DSF FHIR server
	 * @throws IllegalArgumentException
	 *             if the start task has no output parameter with the given <b>outputType</b> or if system, code or
	 *             version of the given <b>outputType</b> is blank
	 */
	void modifyOutput(Coding outputType, Type outputValue) throws WebApplicationException;

	/**
	 * Removes an output parameter of the start task with the given <b>outputType</b>, updates the {@link Task} on the
	 * DSF FHIR server and updates the process variable.
	 *
	 * @param outputType
	 *            not <code>null</code>, must have system and code and version
	 * @throws WebApplicationException
	 *             if start task can not be update at the DSF FHIR server
	 * @throws IllegalArgumentException
	 *             if the start task has no output parameter with the given <b>outputType</b> or if system, code or
	 *             version of the given <b>outputType</b> is blank
	 */
	void removeOutput(Coding outputType) throws WebApplicationException;

	/**
	 * Removes an output parameter of the start task with the given <b>outputType</b>, updates the {@link Task} on the
	 * DSF FHIR server and updates the process variable.
	 *
	 * @param outputTypeSystem
	 *            not <code>null</code>, not blank
	 * @param outputTypeCode
	 *            not <code>null</code>, not blank
	 * @param outputTypeVersion
	 *            not <code>null</code>, not blank
	 * @throws WebApplicationException
	 *             if start task can not be update at the DSF FHIR server
	 * @throws IllegalArgumentException
	 *             if the start task has no output parameter with the given <b>outputType</b> or if system, code or
	 *             version of the given <b>outputType</b> is blank
	 */
	default void removeOutput(String outputTypeSystem, String outputTypeCode, String outputTypeVersion)
			throws WebApplicationException
	{
		Objects.requireNonNull(outputTypeSystem, "outputTypeSystem");
		Objects.requireNonNull(outputTypeCode, "outputTypeCode");
		Objects.requireNonNull(outputTypeVersion, "outputTypeVersion");

		if (outputTypeSystem.isBlank())
			throw new IllegalArgumentException("outputTypeSystem is blank");
		if (outputTypeCode.isBlank())
			throw new IllegalArgumentException("outputTypeCode is blank");
		if (outputTypeVersion.isBlank())
			throw new IllegalArgumentException("outputTypeVersion is blank");

		removeOutput(new Coding(outputTypeSystem, outputTypeCode, null).setVersion(outputTypeVersion));
	}
}
