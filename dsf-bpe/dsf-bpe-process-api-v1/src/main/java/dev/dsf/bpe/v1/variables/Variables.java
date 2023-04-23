package dev.dsf.bpe.v1.variables;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.camunda.bpm.engine.variable.value.TypedValue;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Task;

//TODO JavaDoc for all methods with example use
public interface Variables
{
	void setAlternativeBusinessKey(String alternativeBusinessKey);

	Target createUniDirectionalTarget(String targetOrganizationIdentifierValue, String targetEndpointIdentifierValue,
			String targetEndpointUrl);

	Target createBiDirectionalTarget(String targetOrganizationIdentifierValue, String targetEndpointIdentifierValue,
			String targetEndpointUrl, String correlationKey);

	void setTarget(Target target);

	Target getTarget();

	default Targets createTargets(Target... targets)
	{
		return createTargets(Arrays.asList(targets));
	}

	Targets createTargets(List<? extends Target> targets);

	void setTargets(Targets targets);

	Targets getTargets();

	void setResourceList(String variableName, List<? extends Resource> resources);

	<R extends Resource> List<R> getResourceList(String variableName);

	void setResource(String variableName, Resource resource);

	<R extends Resource> R getResource(String variableName);

	Task getMainTask();

	Task getLatestTask();

	List<Task> getTasks();
	
	List<Task> getCurrentTasks();

	void updateTask(Task task);

	// basic types

	void setVariable(String variableName, TypedValue value);

	Object getVariable(String variableName);

	default void setInteger(String variableName, Integer value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.integerValue(value));
	}

	default Integer getInteger(String variableName)
	{
		return (Integer) getVariable(variableName);
	}

	default void setString(String variableName, String value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.stringValue(value));
	}

	default String getString(String variableName)
	{
		return (String) getVariable(variableName);
	}

	default void setBoolean(String variableName, Boolean value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.booleanValue(value));
	}

	default Boolean getBoolean(String variableName)
	{
		return (Boolean) getVariable(variableName);
	}

	default void setByteArray(String variableName, byte[] value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.byteArrayValue(value));
	}

	default byte[] getByteArray(String variableName)
	{
		return (byte[]) getVariable(variableName);
	}

	default void setDate(String variableName, Date value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.dateValue(value));
	}

	default Date getDate(String variableName)
	{
		return (Date) getVariable(variableName);
	}

	default void setLong(String variableName, Long value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.longValue(value));
	}

	default Long getLong(String variableName)
	{
		return (Long) getVariable(variableName);
	}

	default void setShort(String variableName, Short value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.shortValue(value));
	}

	default Short getShort(String variableName)
	{
		return (Short) getVariable(variableName);
	}

	default void setDouble(String variableName, Double value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.doubleValue(value));
	}

	default Double getDouble(String variableName)
	{
		return (Double) getVariable(variableName);
	}

	default void setNumber(String variableName, Number value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.numberValue(value));
	}

	default Number getNumber(String variableName)
	{
		return (Number) getVariable(variableName);
	}

	default void setFile(String variableName, File value)
	{
		setVariable(variableName, org.camunda.bpm.engine.variable.Variables.fileValue(value));
	}

	default File getFile(String variableName)
	{
		return (File) getVariable(variableName);
	}
}
