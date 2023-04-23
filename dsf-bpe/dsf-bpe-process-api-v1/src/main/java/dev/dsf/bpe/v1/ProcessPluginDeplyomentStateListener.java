package dev.dsf.bpe.v1;

import java.util.List;

public interface ProcessPluginDeplyomentStateListener
{
	void onProcessesDeployed(List<String> processes);
}
