package dev.dsf.bpe.camunda;

import java.util.List;

import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.delegate.VariableScope;
import org.camunda.bpm.engine.impl.bpmn.parser.FieldDeclaration;

import dev.dsf.bpe.api.plugin.ProcessIdAndVersion;

public interface DelegateProvider extends ProcessPluginConsumer
{
	Class<?> getDefaultUserTaskListenerClass(ProcessIdAndVersion processKeyAndVersion);

	boolean isDefaultUserTaskListenerOrSuperClassOf(ProcessIdAndVersion processKeyAndVersion, String className);

	JavaDelegate getMessageSendTask(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope);

	JavaDelegate getServiceTask(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope);

	JavaDelegate getMessageEndEvent(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope);

	JavaDelegate getMessageIntermediateThrowEvent(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope);

	ExecutionListener getExecutionListener(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope);

	TaskListener getTaskListener(ProcessIdAndVersion processIdAndVersion, String className,
			List<FieldDeclaration> fieldDeclarations, VariableScope variableScope);
}
