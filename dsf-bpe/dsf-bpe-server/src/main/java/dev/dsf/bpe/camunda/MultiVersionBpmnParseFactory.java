package dev.dsf.bpe.camunda;

import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParser;
import org.camunda.bpm.engine.impl.cfg.BpmnParseFactory;

public class MultiVersionBpmnParseFactory implements BpmnParseFactory
{
	private final DelegateProvider delegateProvider;

	public MultiVersionBpmnParseFactory(DelegateProvider delegateProvider)
	{
		this.delegateProvider = delegateProvider;
	}

	@Override
	public BpmnParse createBpmnParse(BpmnParser bpmnParser)
	{
		return new MultiVersionBpmnParse(bpmnParser, delegateProvider);
	}
}
