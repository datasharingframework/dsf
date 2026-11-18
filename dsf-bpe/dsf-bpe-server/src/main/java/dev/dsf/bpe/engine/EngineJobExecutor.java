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
package dev.dsf.bpe.engine;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.operaton.bpm.engine.impl.jobexecutor.DefaultJobExecutor;

public class EngineJobExecutor extends DefaultJobExecutor
{
	@Override
	protected void startExecutingJobs()
	{
		if (threadPoolExecutor == null || threadPoolExecutor.isShutdown())
		{
			threadPoolExecutor = new ThreadPoolExecutor(getCorePoolSize(), getMaxPoolSize(), 0L, TimeUnit.MILLISECONDS,
					new ArrayBlockingQueue<>(getQueueSize()),
					Thread.ofPlatform().name("process-engine-job-executor-", 0).factory());
			threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
		}

		super.startExecutingJobs();
	}
}
