package dev.dsf.bpe.v2.service;

import java.util.Optional;
import java.util.stream.Stream;

import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Task;
import org.hl7.fhir.r4.model.Task.ParameterComponent;
import org.hl7.fhir.r4.model.Task.TaskOutputComponent;
import org.hl7.fhir.r4.model.Type;

/**
 * Methods for manipulating {@link Task} resources.
 */
public interface TaskHelper
{
	/**
	 * @param task
	 *            may be <code>null</code>
	 * @return <code>null</code> if the given <b>task</b> is <code>null</code>
	 */
	String getLocalVersionlessAbsoluteUrl(Task task);


	/**
	 * Returns the first input parameter value from the given <b>task</b> with the given <b>coding</b> (system, code),
	 * if the value of the input parameter is of type 'string'.
	 *
	 * @param task
	 *            may be <code>null</code>
	 * @param coding
	 *            may be <code>null</code>
	 * @return {@link Optional#empty()} if the given <b>task</b> or <b>coding</b> is <code>null</code>
	 * @see ParameterComponent#getType()
	 * @see StringType
	 */
	default Optional<String> getFirstInputParameterStringValue(Task task, Coding coding)
	{
		return getInputParameterStringValues(task, coding).findFirst();
	}

	/**
	 * Returns the first input parameter value from the given <b>task</b> with the given <b>system</b> and <b>code</b>,
	 * if the value of the input parameter is of type 'string'.
	 *
	 * @param task
	 *            may be <code>null</code>
	 * @param system
	 *            may be <code>null</code>
	 * @param code
	 *            may be <code>null</code>
	 * @return {@link Optional#empty()} if the given <b>task</b> is <code>null</code>
	 * @see ParameterComponent#getType()
	 * @see StringType
	 */
	default Optional<String> getFirstInputParameterStringValue(Task task, String system, String code)
	{
		return getInputParameterStringValues(task, system, code).findFirst();
	}

	/**
	 * Returns the first input parameter value from the given <b>task</b> with the given <b>coding</b> (system, code),
	 * if the value of the input parameter has the given <b>expectedType</b>.
	 *
	 * @param <T>
	 *            input parameter value type
	 * @param task
	 *            may be <code>null</code>
	 * @param coding
	 *            may be <code>null</code>
	 * @param expectedType
	 *            not <code>null</code>
	 * @return {@link Optional#empty()} if the given <b>task</b> or <b>coding</b> is <code>null</code>
	 * @see ParameterComponent#getType()
	 * @see Type
	 * @throws NullPointerException
	 *             if the given <b>expectedType</b> is <code>null</code>
	 */
	default <T extends Type> Optional<T> getFirstInputParameterValue(Task task, Coding coding, Class<T> expectedType)
	{
		return getInputParameterValues(task, coding, expectedType).findFirst();
	}

	/**
	 * Returns the first input parameter value from the given <b>task</b> with the given <b>system</b> and <b>code</b>,
	 * if the value of the input parameter has the given <b>expectedType</b>.
	 *
	 * @param <T>
	 *            input parameter value type
	 * @param task
	 *            may be <code>null</code>
	 * @param system
	 *            may be <code>null</code>
	 * @param code
	 *            may be <code>null</code>
	 * @param expectedType
	 *            not <code>null</code>
	 * @return {@link Optional#empty()} if the given <b>task</b> is <code>null</code>
	 * @see ParameterComponent#getType()
	 * @see Type
	 * @throws NullPointerException
	 *             if the given <b>expectedType</b> is <code>null</code>
	 */
	default <T extends Type> Optional<T> getFirstInputParameterValue(Task task, String system, String code,
			Class<T> expectedType)
	{
		return getInputParameterValues(task, system, code, expectedType).findFirst();
	}

	/**
	 * Returns the first input parameter from the given <b>task</b> with the given <b>coding</b> (system, code), if the
	 * value of the input parameter has the given <b>expectedType</b> and the input parameter has an extension with the
	 * given <b>extensionUrl</b>.
	 *
	 * @param task
	 *            may be <code>null</code>
	 * @param coding
	 *            may be <code>null</code>
	 * @param expectedType
	 *            not <code>null</code>
	 * @param extensionUrl
	 *            may be <code>null</code>
	 * @return {@link Optional#empty()} if the given <b>task</b> or <b>coding</b> is <code>null</code>
	 * @see ParameterComponent#getType()
	 * @see Type
	 * @throws NullPointerException
	 *             if the given <b>expectedType</b> is <code>null</code>
	 */
	default Optional<ParameterComponent> getFirstInputParameterWithExtension(Task task, Coding coding,
			Class<? extends Type> expectedType, String extensionUrl)
	{
		return getInputParametersWithExtension(task, coding, expectedType, extensionUrl).findFirst();
	}

	/**
	 * Returns the first input parameter from the given <b>task</b> with the given <b>system</b> and <b>code</b>, if the
	 * value of the input parameter has the given <b>expectedType</b> and the input parameter has an extension with the
	 * given <b>extensionUrl</b>.
	 *
	 * @param task
	 *            may be <code>null</code>
	 * @param system
	 *            may be <code>null</code>
	 * @param code
	 *            may be <code>null</code>
	 * @param expectedType
	 *            not <code>null</code>
	 * @param extensionUrl
	 *            may be <code>null</code>
	 * @return {@link Optional#empty()} if the given <b>task</b> is <code>null</code>
	 * @see ParameterComponent#getType()
	 * @see Type
	 * @throws NullPointerException
	 *             if the given <b>expectedType</b> is <code>null</code>
	 */
	default Optional<ParameterComponent> getFirstInputParameterWithExtension(Task task, String system, String code,
			Class<? extends Type> expectedType, String extensionUrl)
	{
		return getInputParametersWithExtension(task, system, code, expectedType, extensionUrl).findFirst();
	}

	/**
	 * Returns the first input parameter from the given <b>task</b> with the given <b>coding</b> (system, code), if the
	 * value of the input parameter has the given <b>expectedType</b>.
	 *
	 * @param task
	 *            may be <code>null</code>
	 * @param coding
	 *            may be <code>null</code>
	 * @param expectedType
	 *            not <code>null</code>
	 * @return {@link Optional#empty()} if the given <b>task</b> or <b>coding</b> is <code>null</code>
	 * @see ParameterComponent#getType()
	 * @see Type
	 * @throws NullPointerException
	 *             if the given <b>expectedType</b> is <code>null</code>
	 */
	default Optional<ParameterComponent> getFirstInputParameter(Task task, Coding coding,
			Class<? extends Type> expectedType)
	{
		return getInputParameters(task, coding, expectedType).findFirst();
	}

	/**
	 * Returns the first input parameter from the given <b>task</b> with the given <b>system</b> and <b>code</b>, if the
	 * value of the input parameter has the given <b>expectedType</b>.
	 *
	 * @param task
	 *            may be <code>null</code>
	 * @param system
	 *            may be <code>null</code>
	 * @param code
	 *            may be <code>null</code>
	 * @param expectedType
	 *            not <code>null</code>
	 * @return {@link Optional#empty()} if the given <b>task</b> is <code>null</code>
	 * @see ParameterComponent#getType()
	 * @see Type
	 * @throws NullPointerException
	 *             if the given <b>expectedType</b> is <code>null</code>
	 */
	default Optional<ParameterComponent> getFirstInputParameter(Task task, String system, String code,
			Class<? extends Type> expectedType)
	{
		return getInputParameters(task, system, code, expectedType).findFirst();
	}


	/**
	 * Returns input parameter values from the given <b>task</b> with the given <b>coding</b> (system, code), if the
	 * value of the input parameter is of type 'string'.
	 *
	 * @param task
	 *            may be <code>null</code>
	 * @param coding
	 *            may be <code>null</code>
	 * @return {@link Stream#empty()} if the given <b>task</b> or <b>coding</b> is <code>null</code>
	 * @see ParameterComponent#getType()
	 * @see StringType
	 */
	Stream<String> getInputParameterStringValues(Task task, Coding coding);

	/**
	 * Returns input parameter values from the given <b>task</b> with the given <b>system</b> and <b>code</b>, if the
	 * value of the input parameter is of type 'string'.
	 *
	 * @param task
	 *            may be <code>null</code>
	 * @param system
	 *            may be <code>null</code>
	 * @param code
	 *            may be <code>null</code>
	 * @return {@link Stream#empty()} if the given <b>task</b> is <code>null</code>
	 * @see ParameterComponent#getType()
	 * @see StringType
	 */
	Stream<String> getInputParameterStringValues(Task task, String system, String code);

	/**
	 * Returns input parameter values from the given <b>task</b> with the given <b>coding</b> (system, code), if the
	 * value of the input parameter has the given <b>expectedType</b>.
	 *
	 * @param <T>
	 *            input parameter value type
	 * @param task
	 *            may be <code>null</code>
	 * @param coding
	 *            may be <code>null</code>
	 * @param expectedType
	 *            not <code>null</code>
	 * @return {@link Stream#empty()} if the given <b>task</b> or <b>coding</b> is <code>null</code>
	 * @throws NullPointerException
	 *             if the given <b>expectedType</b> is <code>null</code>
	 * @see ParameterComponent#getType()
	 * @see Type
	 */
	<T extends Type> Stream<T> getInputParameterValues(Task task, Coding coding, Class<T> expectedType);

	/**
	 * Returns input parameter values from the given <b>task</b> with the given <b>system</b> and <b>code</b>, if the
	 * value of the input parameter has the given <b>expectedType</b>.
	 *
	 * @param <T>
	 *            input parameter value type
	 * @param task
	 *            may be <code>null</code>
	 * @param system
	 *            may be <code>null</code>
	 * @param code
	 *            may be <code>null</code>
	 * @param expectedType
	 *            not <code>null</code>
	 * @return {@link Stream#empty()} if the given <b>task</b> is <code>null</code>
	 * @throws NullPointerException
	 *             if the given <b>expectedType</b> is <code>null</code>
	 * @see ParameterComponent#getType()
	 * @see Type
	 */
	<T extends Type> Stream<T> getInputParameterValues(Task task, String system, String code, Class<T> expectedType);

	/**
	 * Returns input parameters from the given <b>task</b> with the given <b>coding</b> (system, code), if the value of
	 * the input parameter has the given <b>expectedType</b> and the input parameter has an extension with the given
	 * <b>extensionUrl</b>.
	 *
	 * @param task
	 *            may be <code>null</code>
	 * @param coding
	 *            may be <code>null</code>
	 * @param expectedType
	 *            not <code>null</code>
	 * @param extensionUrl
	 *            may be <code>null</code>
	 * @return {@link Stream#empty()} if the given <b>task</b> or <b>coding</b> is <code>null</code>
	 * @throws NullPointerException
	 *             if the given <b>expectedType</b> is <code>null</code>
	 * @see ParameterComponent#getType()
	 * @see Type
	 */
	Stream<ParameterComponent> getInputParametersWithExtension(Task task, Coding coding,
			Class<? extends Type> expectedType, String extensionUrl);

	/**
	 * Returns input parameters from the given <b>task</b> with the given <b>system</b> and <b>code</b>, if the value of
	 * the input parameter has the given <b>expectedType</b> and the input parameter has an extension with the given
	 * <b>extensionUrl</b>.
	 *
	 * @param task
	 *            may be <code>null</code>
	 * @param system
	 *            may be <code>null</code>
	 * @param code
	 *            may be <code>null</code>
	 * @param expectedType
	 *            not <code>null</code>
	 * @param extensionUrl
	 *            may be <code>null</code>
	 * @return {@link Stream#empty()} if the given <b>task</b> is <code>null</code>
	 * @throws NullPointerException
	 *             if the given <b>expectedType</b> is <code>null</code>
	 * @see ParameterComponent#getType()
	 * @see Type
	 */
	Stream<ParameterComponent> getInputParametersWithExtension(Task task, String system, String code,
			Class<? extends Type> expectedType, String extensionUrl);

	/**
	 * Returns the input parameters from the given <b>task</b> with the given <b>coding</b> (system, code), if the value
	 * of the input parameter has the given <b>expectedType</b>.
	 *
	 * @param task
	 *            may be <code>null</code>
	 * @param coding
	 *            may be <code>null</code>
	 * @param expectedType
	 *            not <code>null</code>
	 * @return {@link Stream#empty()} if the given <b>task</b> or <b>coding</b> is <code>null</code>
	 * @throws NullPointerException
	 *             if the given <b>expectedType</b> is <code>null</code>
	 * @see ParameterComponent#getType()
	 * @see Type
	 */
	Stream<ParameterComponent> getInputParameters(Task task, Coding coding, Class<? extends Type> expectedType);

	/**
	 * Returns the input parameters from the given <b>task</b> with the given <b>system</b> and <b>code</b>, if the
	 * value of the input parameter has the given <b>expectedType</b>.
	 *
	 * @param task
	 *            may be <code>null</code>
	 * @param system
	 *            may be <code>null</code>
	 * @param code
	 *            may be <code>null</code>
	 * @param expectedType
	 *            not <code>null</code>
	 * @return {@link Stream#empty()} if the given <b>task</b> is <code>null</code>
	 * @throws NullPointerException
	 *             if the given <b>expectedType</b> is <code>null</code>
	 * @see ParameterComponent#getType()
	 * @see Type
	 */
	Stream<ParameterComponent> getInputParameters(Task task, String system, String code,
			Class<? extends Type> expectedType);


	/**
	 * Creates an input parameter for the given <b>value</b> and <b>coding</b>.
	 *
	 * @param value
	 *            may be <code>null</code>
	 * @param coding
	 *            may be <code>null</code>
	 * @return not <code>null</code>
	 * @see ParameterComponent#setType(org.hl7.fhir.r4.model.CodeableConcept)
	 * @see ParameterComponent#setValue(Type)
	 */
	ParameterComponent createInput(Type value, Coding coding);

	/**
	 * Creates an input parameter for the given <b>value</b>, <b>system</b> and <b>code</b>.
	 *
	 * @param value
	 *            may be <code>null</code>
	 * @param system
	 *            may be <code>null</code>
	 * @param code
	 *            may be <code>null</code>
	 * @param version
	 *            may be <code>null</code>
	 * @return not <code>null</code>
	 * @see ParameterComponent#setType(org.hl7.fhir.r4.model.CodeableConcept)
	 * @see ParameterComponent#setValue(Type)
	 */
	default ParameterComponent createInput(Type value, String system, String code, String version)
	{
		return createInput(value, new Coding(system, code, null).setVersion(version));
	}


	/**
	 * Creates an output parameter for the given <b>value</b> and <b>coding</b>.
	 *
	 * @param value
	 *            may be <code>null</code>
	 * @param coding
	 *            may be <code>null</code>
	 * @return not <code>null</code>
	 * @see TaskOutputComponent#setType(org.hl7.fhir.r4.model.CodeableConcept)
	 * @see TaskOutputComponent#setValue(Type)
	 */
	TaskOutputComponent createOutput(Type value, Coding coding);

	/**
	 * Creates an output parameter for the given <b>value</b>, <b>system</b> and <b>code</b>.
	 *
	 * @param value
	 *            may be <code>null</code>
	 * @param system
	 *            may be <code>null</code>
	 * @param code
	 *            may be <code>null</code>
	 * @param version
	 *            may be <code>null</code>
	 * @return not <code>null</code>
	 * @see TaskOutputComponent#setType(org.hl7.fhir.r4.model.CodeableConcept)
	 * @see TaskOutputComponent#setValue(Type)
	 */
	default TaskOutputComponent createOutput(Type value, String system, String code, String version)
	{
		return createOutput(value, new Coding(system, code, null).setVersion(version));
	}
}
