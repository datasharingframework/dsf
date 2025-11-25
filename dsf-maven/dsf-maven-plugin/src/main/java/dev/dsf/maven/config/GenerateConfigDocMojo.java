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
package dev.dsf.maven.config;

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Generates configuration documentation for the DSF and process plugins.
 */
@Mojo(name = "generate-config-doc", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class GenerateConfigDocMojo extends AbstractMojo
{
	@Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
	private File projectBuildDirectory;

	@Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
	private List<String> compileClasspathElements;

	/**
	 * The packages to scan for configuration classes.
	 */
	@Parameter(property = "dsf.configDocPackages", required = true)
	private List<String> configDocPackages;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		getLog().debug("projectBuildDirectory: " + projectBuildDirectory);
		getLog().debug("compileClasspathElements: " + compileClasspathElements);
		getLog().debug("configDocPackages: " + configDocPackages);

		new ConfigDocGenerator(projectBuildDirectory.toPath(), compileClasspathElements)
				.generateDocumentation(configDocPackages);
	}
}
