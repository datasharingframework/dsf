package dev.dsf.bpe.v1;

import java.util.List;

import org.springframework.context.annotation.Bean;

/**
 * Listener called after process plugin deployment with a list of deployed process-ids from this plugin. List contains
 * all processes deployed in the bpe depending on the exclusion and retired config.
 * <p>
 * Register a singleton {@link Bean} implementing this interface to execute custom code like connection tests if a
 * process has been deployed.
 */
public interface ProcessPluginDeplyomentStateListener
{
	void onProcessesDeployed(List<String> processes);
}
