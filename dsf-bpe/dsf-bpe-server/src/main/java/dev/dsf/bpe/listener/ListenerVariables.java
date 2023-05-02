package dev.dsf.bpe.listener;

import org.hl7.fhir.r4.model.Task;

import dev.dsf.bpe.v1.variables.Variables;

public interface ListenerVariables extends Variables
{
	void onStart(Task task);

	void onContinue(Task task);

	void onEnd();
}
