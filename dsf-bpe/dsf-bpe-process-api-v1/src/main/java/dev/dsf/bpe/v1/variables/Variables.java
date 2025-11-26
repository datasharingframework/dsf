/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.dsf.bpe.v1.variables;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.camunda.bpm.engine.variable.value.TypedValue;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;

import dev.dsf.bpe.v1.constants.BpmnExecutionVariables;

/**
 * Gives access to process execution variables. Includes factory methods for {@link Target} and {@link Targets} values.
 */
public interface Variables
{
	/**
	 * Sets execution variable {@link BpmnExecutionVariables#ALTERNATIVE_BUSINESS_KEY}
	 *
	 * @param alternativeBusinessKey
	 *            may be <code>null</code>
	 */
	void setAlternativeBusinessKey(String alternativeBusinessKey);

	/**
	 * Creates a new {@link Target} object.
	 * <p>
	 * <i>A not</i> <code>null</code> <i><b>correlationKey</b> should be used if return messages aka. Task resources
	 * from multiple organizations with the same message-name are expected in a following multi instance message receive
	 * task or intermediate message catch event in a multi instance subprocess.<br>
	 * Note: The correlationKey needs to be set as a {@link BpmnExecutionVariables#CORRELATION_KEY} variable in the
	 * message receive task or intermediate message catch event of a subprocess before incoming messages aka. Task
	 * resources can be correlated. Within a BPMN file this can be accomplished by setting an input variable with name:
	 * {@link BpmnExecutionVariables#CORRELATION_KEY}, type:</i> <code>string or expression</code><i>, and value:
	 * </i><code>${target.correlationKey}</code>.
	 * <p>
	 * <i>A not</i> <code>null</code> <i><b>correlationKey</b> should also be used when sending a message aka. Task
	 * resource back to an organization waiting for multiple returns.</i>
	 *
	 * @param organizationIdentifierValue
	 *            not <code>null</code>
	 * @param endpointIdentifierValue
	 *            not <code>null</code>
	 * @param endpointAddress
	 *            not <code>null</code>
	 * @param correlationKey
	 *            not <code>null</code> if used for sending multiple messages and multiple messages with the same
	 *            message-name are expected in return
	 * @return new {@link Target} object
	 * @see #createTarget(String, String, String)
	 * @see #setTarget(Target)
	 */
	Target createTarget(String organizationIdentifierValue, String endpointIdentifierValue, String endpointAddress,
			String correlationKey);

	/**
	 * Creates a new {@link Target} object.
	 *
	 * See {@link #createTarget(String, String, String, String)} for sending a correlation-key for 1:n or n:1
	 * relationships.
	 *
	 * @param organizationIdentifierValue
	 *            not <code>null</code>
	 * @param endpointIdentifierValue
	 *            not <code>null</code>
	 * @param endpointAddress
	 *            not <code>null</code>
	 * @return new {@link Target} object
	 * @see #createTarget(String, String, String, String)
	 * @see #setTarget(Target)
	 */
	default Target createTarget(String organizationIdentifierValue, String endpointIdentifierValue,
			String endpointAddress)
	{
		return createTarget(organizationIdentifierValue, endpointIdentifierValue, endpointAddress, null);
	}

	/**
	 * Sets execution variable {@link BpmnExecutionVariables#TARGET}
	 *
	 * @param target
	 *            may be <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the given <b>target</b> object is not supported, meaning the object was not created by this
	 *             {@link Variables} implementation
	 * @see #createTarget(String, String, String)
	 * @see #createTarget(String, String, String, String)
	 * @see #getTarget()
	 */
	void setTarget(Target target) throws IllegalArgumentException;

	/**
	 * Retrieves execution variable {@link BpmnExecutionVariables#TARGET}
	 *
	 * @return Execution variable {@link BpmnExecutionVariables#TARGET}, may be <code>null</code>
	 */
	Target getTarget();

	/**
	 * Creates a new target list.
	 *
	 * <i>Use</i> <code>${targets.entries}</code> <i>as a multi instance collection and</i> <code>target</code> <i>as
	 * the element variable to loop over this list in a multi instance task or subprocess.</i>
	 *
	 * @param targets
	 *            {@link Target} objects to incorporate into the created list
	 * @return a new target list
	 * @throws IllegalArgumentException
	 *             if one of the given <b>target</b> objects is not supported, meaning the object was not created by
	 *             this {@link Variables} implementation
	 * @see #createTarget(String, String, String)
	 * @see #createTarget(String, String, String, String)
	 * @see #setTargets(Targets)
	 */
	default Targets createTargets(Target... targets)
	{
		return createTargets(Arrays.asList(targets));
	}

	/**
	 * Creates a new target list.
	 *
	 * <i>Use</i> <code>${targets.entries}</code> <i>as a multi instance collection and</i> <code>target</code> <i>as
	 * the element variable to loop over this list in a multi instance task or subprocess.</i>
	 *
	 * @param targets
	 *            {@link Target} objects to incorporate into the created list, may be <code>null</code>
	 * @return a new target list
	 * @throws IllegalArgumentException
	 *             if one of the given <b>target</b> objects is not supported, meaning the object was not created by
	 *             this {@link Variables} implementation
	 * @see #createTarget(String, String, String)
	 * @see #createTarget(String, String, String, String)
	 * @see #setTargets(Targets)
	 */
	Targets createTargets(List<? extends Target> targets);

	/**
	 * Sets execution variable {@link BpmnExecutionVariables#TARGETS}.
	 *
	 * <i>Use</i> <code>${targets.entries}</code> <i>as a multi instance collection and</i>
	 *
	 * @param targets
	 *            may be <code>null</code>
	 * @see #createTargets(List)
	 * @see #createTargets(Target...)
	 * @see #getTargets()
	 */
	void setTargets(Targets targets);

	/**
	 * Retrieves execution variable {@link BpmnExecutionVariables#TARGETS}
	 *
	 * @return Execution variable {@link BpmnExecutionVariables#TARGETS}, may be <code>null</code>
	 * @see #setTargets(Targets)
	 */
	Targets getTargets();

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given FHIR {@link Resource} list
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param resources
	 */
	void setResourceList(String variableName, List<? extends Resource> resources);

	/**
	 * Retrieves FHIR {@link Resource} list execution variable with the given <b>variableName</b>
	 *
	 * @param <R>
	 *            FHIR resource type
	 * @param variableName
	 *            not <code>null</code>
	 * @return list of FHIR resources from execution variables for the given <b>variableName</b>, may be
	 *         <code>null</code>
	 */
	<R extends Resource> List<R> getResourceList(String variableName);

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given FHIR {@link Resource}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param resource
	 *            may be <code>null</code>
	 */
	void setResource(String variableName, Resource resource);

	/**
	 * Retrieves FHIR {@link Resource} execution variable with the given <b>variableName</b>
	 *
	 * @param <R>
	 *            FHIR resource type
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 */
	<R extends Resource> R getResource(String variableName);

	/**
	 * Returns the {@link Task} associated with the message start event of the process.
	 *
	 * @return {@link Task} that started the process instance, not <code>null</code>
	 * @see #updateTask(Task)
	 * @see #getLatestTask()
	 * @see #getTasks()
	 */
	Task getStartTask();

	/**
	 * Returns the latest {@link Task} received by this process or subprocess via a intermediate message catch event or
	 * message receive task.
	 *
	 * @return Last received {@link Task} of the current process or subprocess, not <code>null</code>
	 * @see #updateTask(Task)
	 * @see #getStartTask()
	 * @see #getCurrentTasks()
	 */
	Task getLatestTask();

	/**
	 * @return All {@link Task} resources received
	 * @see #getCurrentTasks()
	 */
	List<Task> getTasks();

	/**
	 * @return All {@link Task} resources received by the current process or subprocess
	 * @see #getTasks()
	 */
	List<Task> getCurrentTasks();

	/**
	 * Does nothing if the given <b>task</b> is <code>null</code>. Forces an update to the Task list variable used
	 * internally to track all received Task resources if the given <b>task</b> object is already part of this list.
	 *
	 * @param task
	 *            may be <code>null</code>
	 * @see #getStartTask()
	 * @see #getLatestTask()
	 * @see #getTasks()
	 * @see #getCurrentTasks()
	 */
	void updateTask(Task task);

	/**
	 * @return Last received {@link QuestionnaireResponse}, <code>null</code> if nothing received yet
	 */
	QuestionnaireResponse getLatestReceivedQuestionnaireResponse();

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link TypedValue}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getVariable(String)
	 * @see #setInteger(String, Integer)
	 * @see #setString(String, String)
	 * @see #setBoolean(String, Boolean)
	 * @see #setByteArray(String, byte[])
	 * @see #setDate(String, Date)
	 * @see #setLong(String, Long)
	 * @see #setShort(String, Short)
	 * @see #setDouble(String, Double)
	 * @see #setNumber(String, Number)
	 * @see #setFile(String, File)
	 */
	void setVariable(String variableName, TypedValue value);

	/**
	 * Retrieves execution variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @see #setVariable(String, TypedValue)
	 * @see #getInteger(String)
	 * @see #getString(String)
	 * @see #getBoolean(String)
	 * @see #getByteArray(String)
	 * @see #getDate(String)
	 * @see #getLong(String)
	 * @see #getShort(String)
	 * @see #getDouble(String)
	 * @see #getNumber(String)
	 * @see #getFile(String)
	 */
	Object getVariable(String variableName);

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link Integer}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getInteger(String)
	 * @see #setVariable(String, TypedValue)
	 */
	default void setInteger(String variableName, Integer value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.integerValue(value));
	}

	/**
	 * Retrieves {@link Integer} execution variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link Integer}
	 * @see #setInteger(String, Integer)
	 * @see #getVariable(String)
	 */
	default Integer getInteger(String variableName)
	{
		return (Integer) getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link String}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getString(String)
	 * @see #setVariable(String, TypedValue)
	 */
	default void setString(String variableName, String value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.stringValue(value));
	}

	/**
	 * Retrieves {@link String} execution variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link String}
	 * @see #setString(String, String)
	 * @see #getVariable(String)
	 */
	default String getString(String variableName)
	{
		return (String) getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link Boolean}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getBoolean(String)
	 * @see #setVariable(String, TypedValue)
	 */
	default void setBoolean(String variableName, Boolean value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.booleanValue(value));
	}

	/**
	 * Retrieves {@link Boolean} execution variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link Boolean}
	 * @see #setBoolean(String, Boolean)
	 * @see #getVariable(String)
	 */
	default Boolean getBoolean(String variableName)
	{
		return (Boolean) getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given <code>byte[]</code>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getByteArray(String)
	 * @see #setVariable(String, TypedValue)
	 */
	default void setByteArray(String variableName, byte[] value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.byteArrayValue(value));
	}

	/**
	 * Retrieves <code>byte[]</code> execution variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a <code>byte[]</code>
	 * @see #setByteArray(String, byte[])
	 * @see #getVariable(String)
	 */
	default byte[] getByteArray(String variableName)
	{
		return (byte[]) getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link Date}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getDate(String)
	 * @see #setVariable(String, TypedValue)
	 */
	default void setDate(String variableName, Date value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.dateValue(value));
	}

	/**
	 * Retrieves {@link Date} execution variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link Date}
	 * @see #setDate(String, Date)
	 * @see #getVariable(String)
	 */
	default Date getDate(String variableName)
	{
		return (Date) getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link Long}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getLong(String)
	 * @see #setVariable(String, TypedValue)
	 */
	default void setLong(String variableName, Long value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.longValue(value));
	}

	/**
	 * Retrieves {@link Long} execution variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link Long}
	 * @see #setLong(String, Long)
	 * @see #getVariable(String)
	 */
	default Long getLong(String variableName)
	{
		return (Long) getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link Short}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getShort(String)
	 * @see #setVariable(String, TypedValue)
	 */
	default void setShort(String variableName, Short value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.shortValue(value));
	}

	/**
	 * Retrieves {@link Short} execution variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link Short}
	 * @see #setShort(String, Short)
	 * @see #getVariable(String)
	 */
	default Short getShort(String variableName)
	{
		return (Short) getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link Double}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getDouble(String)
	 * @see #setVariable(String, TypedValue)
	 */
	default void setDouble(String variableName, Double value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.doubleValue(value));
	}

	/**
	 * Retrieves {@link Double} execution variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link Double}
	 * @see #setDouble(String, Double)
	 * @see #getVariable(String)
	 */
	default Double getDouble(String variableName)
	{
		return (Double) getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link Number}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getNumber(String)
	 * @see #setVariable(String, TypedValue)
	 */
	default void setNumber(String variableName, Number value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.numberValue(value));
	}

	/**
	 * Retrieves {@link Number} execution variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link Number}
	 * @see #setNumber(String, Number)
	 * @see #getVariable(String)
	 */
	default Number getNumber(String variableName)
	{
		return (Number) getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link File}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getFile(String)
	 * @see #setVariable(String, TypedValue)
	 */
	default void setFile(String variableName, File value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.fileValue(value));
	}

	/**
	 * Retrieves {@link File} execution variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link File}
	 * @see #setFile(String, File)
	 * @see #getVariable(String)
	 */
	default File getFile(String variableName)
	{
		return (File) getVariable(variableName);
	}
}
