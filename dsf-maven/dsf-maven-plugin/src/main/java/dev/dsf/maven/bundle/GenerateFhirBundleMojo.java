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
package dev.dsf.maven.bundle;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Generates a FHIR Bundle containing all resources found in the configured base folder.
 */
@Mojo(name = "generate-fhir-bundle", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true)
public class GenerateFhirBundleMojo extends AbstractMojo
{
	@Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
	private File projectBaseDir;

	@Parameter(defaultValue = "dsf.baseFolder", required = true)
	private File baseFolder;

	@Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
	private File projectBuildDirectory;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		getLog().debug("projectBaseDir: " + projectBaseDir);
		getLog().debug("baseFolder: " + baseFolder);

		new BundleGenerator(projectBaseDir.toPath(), baseFolder.toPath(), projectBuildDirectory.toPath())
				.generateAndSaveBundle();
	}
}
