package dev.dsf.bpe.camunda;

import org.camunda.bpm.engine.impl.telemetry.dto.TelemetryDataImpl;
import org.camunda.bpm.engine.spring.SpringProcessEngineConfiguration;

public class MultiVersionSpringProcessEngineConfiguration extends SpringProcessEngineConfiguration
{
	public MultiVersionSpringProcessEngineConfiguration(DelegateProvider delegateProvider)
	{
		bpmnParseFactory = new MultiVersionBpmnParseFactory(delegateProvider);
	}

	@Override
	protected void initDiagnostics()
	{
		// override to turn telemetry collection of

		setTelemetryData(new TelemetryDataImpl(null, null));
	}
}