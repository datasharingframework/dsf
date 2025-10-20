package dev.dsf.bpe.api.logging;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.operaton.bpm.engine.delegate.DelegateExecution;
import org.operaton.bpm.engine.delegate.DelegateTask;

public interface PluginMdc
{
	@FunctionalInterface
	public interface ConsumerWithException<T>
	{
		void accept(T t) throws Exception;
	}

	void executeWithProcessMdc(DelegateTask delegateTask, Consumer<DelegateTask> executable);

	void executeWithProcessMdc(DelegateExecution delegateExecution, ConsumerWithException<DelegateExecution> executable)
			throws Exception;

	void executeWithPluginMdc(Runnable runnable);

	boolean executeWithPluginMdc(Supplier<Boolean> supplier);
}