package dev.dsf.bpe.test.deployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dsf.bpe.v2.ProcessPluginDeploymentListener;

public class ProcessPluginDeploymentListenerTestImpl implements ProcessPluginDeploymentListener
{
	private static final Logger logger = LoggerFactory.getLogger(ProcessPluginDeploymentListenerTestImpl.class);

	private final List<Boolean> ok = new ArrayList<>();

	@Override
	public void onProcessesDeployed(List<String> processes)
	{
		ok.add(processes != null && processes.size() == 1 && processes.contains("dsfdev_test"));
		logger.info("Deployed processes: {}", processes);
	}

	public List<Boolean> getOk()
	{
		return Collections.unmodifiableList(ok);
	}
}
