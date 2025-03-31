package dev.dsf.bpe.v2.variables;

import java.io.File;
import java.util.Date;
import java.util.List;

import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.dsf.bpe.v2.activity.task.BusinessKeyStrategies;
import dev.dsf.bpe.v2.constants.BpmnExecutionVariables;

/**
 * Gives access to process execution variables. Includes factory methods for {@link Target} and {@link Targets} values.
 */
public interface Variables
{
	/**
	 * @return not <code>null</code>, business key of the current process instance
	 */
	String getBusinessKey();

	/**
	 * @return not <code>null</code>, id of the current activity
	 */
	String getCurrentActivityId();

	/**
	 * @return not <code>null</code>, id of the current process definition
	 */
	String getProcessDefinitionId();

	/**
	 * @return not <code>null</code>, id of the current activity instance
	 */
	String getActivityInstanceId();

	/**
	 * Sets execution variable {@link BpmnExecutionVariables#ALTERNATIVE_BUSINESS_KEY} to the given
	 * <b>alternativeBusinessKey</b>
	 *
	 * @param alternativeBusinessKey
	 *            may be <code>null</code>
	 * @see BusinessKeyStrategies#ALTERNATIVE
	 */
	void setAlternativeBusinessKey(String alternativeBusinessKey);

	/**
	 * Retrieves execution variable {@link BpmnExecutionVariables#ALTERNATIVE_BUSINESS_KEY}
	 *
	 * @return may be <code>null</code>
	 * @see BusinessKeyStrategies#ALTERNATIVE
	 */
	default String getAlternativeBusinessKey()
	{
		return getString(BpmnExecutionVariables.ALTERNATIVE_BUSINESS_KEY);
	}

	/**
	 * Creates a new {@link Target} object.
	 * <p>
	 * <i>A not</i> <code>null</code> <i><b>correlationKey</b> should be used if return messages i.e. Task resources
	 * from multiple organizations with the same message-name are expected in a following multi instance message receive
	 * task or intermediate message catch event in a multi instance subprocess.<br>
	 * Note: The correlationKey needs to be set as a {@link BpmnExecutionVariables#CORRELATION_KEY} variable in the
	 * message receive task or intermediate message catch event of a subprocess before incoming messages i.e. Task
	 * resources can be correlated. Within a BPMN file this can be accomplished by setting an input variable with name:
	 * {@link BpmnExecutionVariables#CORRELATION_KEY}, type:</i> <code>string or expression</code><i>, and value:
	 * </i><code>${target.correlationKey}</code>.
	 * <p>
	 * <i>A not</i> <code>null</code> <i><b>correlationKey</b> should also be used when sending a message i.e. Task
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
		return createTargets(List.of(targets));
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
	void setFhirResourceList(String variableName, List<? extends Resource> resources);

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
	<R extends Resource> List<R> getFhirResourceList(String variableName);

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given FHIR {@link Resource}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param resource
	 *            may be <code>null</code>
	 */
	void setFhirResource(String variableName, Resource resource);

	/**
	 * Retrieves FHIR {@link Resource} execution variable with the given <b>variableName</b>
	 *
	 * @param <R>
	 *            FHIR resource type
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 */
	<R extends Resource> R getFhirResource(String variableName);

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
	 * Uses {@link ObjectMapper} to serialize the given <b>value</b> into json. Value class needs annotations like
	 * {@link JsonCreator}, {@link JsonProperty} and {@link JsonGetter}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be null
	 * @see #getVariable(String)
	 */
	void setJsonVariable(String variableName, Object value);

	/**
	 * Retrieves execution variable with the given <b>variableName</b>
	 *
	 * @param <T>
	 *            target variable type
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the returned variable can not be cast to &lt;T&gt;
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
	<T> T getVariable(String variableName);

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link Integer}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getInteger(String)
	 */
	void setInteger(String variableName, Integer value);

	/**
	 * Retrieves {@link Integer} execution variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not an {@link Integer}
	 * @see #setInteger(String, Integer)
	 * @see #getVariable(String)
	 */
	default Integer getInteger(String variableName)
	{
		return getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link String}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getString(String)
	 */
	void setString(String variableName, String value);

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
		return getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link Boolean}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getBoolean(String)
	 */
	void setBoolean(String variableName, Boolean value);

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
		return getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given <code>byte[]</code>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getByteArray(String)
	 */
	void setByteArray(String variableName, byte[] value);

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
		return getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link Date}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getDate(String)
	 */
	void setDate(String variableName, Date value);

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
		return getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link Long}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getLong(String)
	 */
	void setLong(String variableName, Long value);

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
		return getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link Short}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getShort(String)
	 */
	void setShort(String variableName, Short value);

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
		return getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link Double}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getDouble(String)
	 */
	void setDouble(String variableName, Double value);

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
		return getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link Number}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getNumber(String)
	 */
	void setNumber(String variableName, Number value);

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
		return getVariable(variableName);
	}

	/**
	 * Sets execution variable with the given <b>variableName</b> to the given {@link File}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getFile(String)
	 */
	void setFile(String variableName, File value);

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
		return getVariable(variableName);
	}

	/**
	 * Sets local variable with the given <b>variableName</b> to the given {@link Integer}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getIntegerLocal(String)
	 */
	void setIntegerLocal(String variableName, Integer value);

	/**
	 * Retrieves {@link Integer} local variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not an {@link Integer}
	 * @see #setIntegerLocal(String, Integer)
	 * @see #getVariableLocal(String)
	 */
	default Integer getIntegerLocal(String variableName)
	{
		return getVariableLocal(variableName);
	}

	/**
	 * Sets local variable with the given <b>variableName</b> to the given {@link String}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getStringLocal(String)
	 */
	void setStringLocal(String variableName, String value);

	/**
	 * Retrieves {@link String} local variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link String}
	 * @see #setStringLocal(String, String)
	 * @see #getVariableLocal(String)
	 */
	default String getStringLocal(String variableName)
	{
		return getVariableLocal(variableName);
	}

	/**
	 * Sets local variable with the given <b>variableName</b> to the given {@link Boolean}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getBooleanLocal(String)
	 */
	void setBooleanLocal(String variableName, Boolean value);

	/**
	 * Retrieves {@link Boolean} local variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link Boolean}
	 * @see #setBooleanLocal(String, Boolean)
	 * @see #getVariableLocal(String)
	 */
	default Boolean getBooleanLocal(String variableName)
	{
		return getVariableLocal(variableName);
	}

	/**
	 * Sets local variable with the given <b>variableName</b> to the given <code>byte[]</code>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getByteArray(String)
	 */
	void setByteArrayLocal(String variableName, byte[] value);

	/**
	 * Retrieves <code>byte[]</code> local variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a <code>byte[]</code>
	 * @see #setByteArrayLocal(String, byte[])
	 * @see #getVariableLocal(String)
	 */
	default byte[] getByteArrayLocal(String variableName)
	{
		return getVariableLocal(variableName);
	}

	/**
	 * Sets local variable with the given <b>variableName</b> to the given {@link Date}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getDateLocal(String)
	 */
	void setDateLocal(String variableName, Date value);

	/**
	 * Retrieves {@link Date} local variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link Date}
	 * @see #setDateLocal(String, Date)
	 * @see #getVariableLocal(String)
	 */
	default Date getDateLocal(String variableName)
	{
		return getVariableLocal(variableName);
	}

	/**
	 * Sets local variable with the given <b>variableName</b> to the given {@link Long}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getLongLocal(String)
	 */
	void setLongLocal(String variableName, Long value);

	/**
	 * Retrieves {@link Long} local variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link Long}
	 * @see #setLongLocal(String, Long)
	 * @see #getVariableLocal(String)
	 */
	default Long getLongLocal(String variableName)
	{
		return getVariableLocal(variableName);
	}

	/**
	 * Sets local variable with the given <b>variableName</b> to the given {@link Short}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getShortLocal(String)
	 */
	void setShortLocal(String variableName, Short value);

	/**
	 * Retrieves {@link Short} local variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link Short}
	 * @see #setShortLocal(String, Short)
	 * @see #getVariableLocal(String)
	 */
	default Short getShortLocal(String variableName)
	{
		return getVariableLocal(variableName);
	}

	/**
	 * Sets local variable with the given <b>variableName</b> to the given {@link Double}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getDoubleLocal(String)
	 */
	void setDoubleLocal(String variableName, Double value);

	/**
	 * Retrieves {@link Double} local variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link Double}
	 * @see #setDoubleLocal(String, Double)
	 * @see #getVariableLocal(String)
	 */
	default Double getDoubleLocal(String variableName)
	{
		return getVariableLocal(variableName);
	}

	/**
	 * Sets local variable with the given <b>variableName</b> to the given {@link Number}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getNumberLocal(String)
	 */
	void setNumberLocal(String variableName, Number value);

	/**
	 * Retrieves {@link Number} local variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link Number}
	 * @see #setNumberLocal(String, Number)
	 * @see #getVariableLocal(String)
	 */
	default Number getNumberLocal(String variableName)
	{
		return getVariableLocal(variableName);
	}

	/**
	 * Sets local variable with the given <b>variableName</b> to the given {@link File}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be <code>null</code>
	 * @see #getFileLocal(String)
	 */
	void setFileLocal(String variableName, File value);

	/**
	 * Retrieves {@link File} local variable with the given <b>variableName</b>
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the stored value is not a {@link File}
	 * @see #setFileLocal(String, File)
	 * @see #getVariableLocal(String)
	 */
	default File getFileLocal(String variableName)
	{
		return getVariableLocal(variableName);
	}

	/**
	 * Uses {@link ObjectMapper} to serialize the given <b>value</b> into json. Value class needs annotations like
	 * {@link JsonCreator}, {@link JsonProperty} and {@link JsonGetter}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param value
	 *            may be null
	 * @see #getVariable(String)
	 */
	void setJsonVariableLocal(String variableName, Object value);

	/**
	 * Retrieves local variable with the given <b>variableName</b>
	 *
	 * @param <T>
	 *            target variable type
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from local variables for the given <b>variableName</b>, may be <code>null</code>
	 * @throws ClassCastException
	 *             if the returned variable can not be cast to &lt;T&gt;
	 * @see #getIntegerLocal(String)
	 * @see #getStringLocal(String)
	 * @see #getBooleanLocal(String)
	 * @see #getByteArrayLocal(String)
	 * @see #getDateLocal(String)
	 * @see #getLongLocal(String)
	 * @see #getShortLocal(String)
	 * @see #getDoubleLocal(String)
	 * @see #getNumberLocal(String)
	 * @see #getFileLocal(String)
	 */
	<T> T getVariableLocal(String variableName);

	/**
	 * Sets local variable with the given <b>variableName</b> to the given FHIR {@link Resource} list
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param resources
	 */
	void setFhirResourceListLocal(String variableName, List<? extends Resource> resources);

	/**
	 * Retrieves FHIR {@link Resource} list local variable with the given <b>variableName</b>
	 *
	 * @param <R>
	 *            FHIR resource type
	 * @param variableName
	 *            not <code>null</code>
	 * @return list of FHIR resources from execution variables for the given <b>variableName</b>, may be
	 *         <code>null</code>
	 */
	<R extends Resource> List<R> getFhirResourceListLocal(String variableName);

	/**
	 * Sets local variable with the given <b>variableName</b> to the given FHIR {@link Resource}
	 *
	 * @param variableName
	 *            not <code>null</code>
	 * @param resource
	 *            may be <code>null</code>
	 */
	void setFhirResourceLocal(String variableName, Resource resource);

	/**
	 * Retrieves FHIR {@link Resource} local variable with the given <b>variableName</b>
	 *
	 * @param <R>
	 *            FHIR resource type
	 * @param variableName
	 *            not <code>null</code>
	 * @return value from execution variables for the given <b>variableName</b>, may be <code>null</code>
	 */
	<R extends Resource> R getFhirResourceLocal(String variableName);
}
