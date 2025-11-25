/*
 * Copyright 2018-2025 Heilbronn University of Applied Sciences
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
public interface ProcessPluginDeploymentStateListener
{
	void onProcessesDeployed(List<String> processes);
}
