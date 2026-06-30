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
package dev.dsf.maven.dev;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Deprecated use goal <code>generate-dev-setup-files</code>
 */
@Mojo(name = "generate-dev-setup-cert-files", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true, aggregator = true)
public class GenerateDevSetupCertFilesMojo extends GenerateDevSetupFilesMojo
{
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		getLog().warn("The goal 'generate-dev-setup-cert-files' is deprecated, use 'generate-dev-setup-files'");

		super.execute();
	}
}
