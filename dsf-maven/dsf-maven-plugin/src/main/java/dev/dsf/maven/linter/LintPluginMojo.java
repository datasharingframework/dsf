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
package dev.dsf.maven.linter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import dev.dsf.linter.DsfLinter;
import dev.dsf.linter.DsfLinter.OverallLinterResult;
import dev.dsf.linter.input.InputResolver;
import dev.dsf.linter.input.InputResolver.ResolutionResult;
import dev.dsf.linter.logger.Logger;

/**
 * Lints DSF process plugin JAR files by validating BPMN processes, FHIR resources and plugin configurations.
 * <p>
 * Runs after the JAR has been built (default phase: verify) and delegates to the
 * <a href="https://github.com/datasharingframework/dsf-linter">DSF Linter</a> core library.
 */
@Mojo(name = "lint", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true)
public class LintPluginMojo extends AbstractMojo
{
	@Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
	private File projectBuildDirectory;

	@Parameter(defaultValue = "${project.build.finalName}", readonly = true, required = true)
	private String finalName;

	@Parameter(property = "dsf.lint.reportPath")
	private File reportPath;

	@Parameter(property = "dsf.lint.html", defaultValue = "false")
	private boolean generateHtmlReport;

	@Parameter(property = "dsf.lint.json", defaultValue = "false")
	private boolean generateJsonReport;

	@Parameter(property = "dsf.lint.failOnErrors", defaultValue = "true")
	private boolean failOnErrors;

	@Parameter(property = "dsf.lint.skip", defaultValue = "false")
	private boolean skip;

	@Parameter(property = "dsf.lint.verbose", defaultValue = "false")
	private boolean verbose;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		if (skip)
		{
			getLog().info("DSF Linter: skipped");
			return;
		}

		Path jarPath = projectBuildDirectory.toPath().resolve(finalName + ".jar");

		if (!Files.exists(jarPath))
		{
			getLog().warn("DSF Linter: JAR not found at " + jarPath + ", skipping");
			return;
		}

		Logger logger = new MavenLinterLogger(getLog(), verbose);

		InputResolver resolver = new InputResolver(logger);
		Optional<ResolutionResult> resolutionResult = resolver.resolve(jarPath.toString());

		if (resolutionResult.isEmpty())
		{
			throw new MojoExecutionException("DSF Linter: failed to resolve JAR file: " + jarPath);
		}

		ResolutionResult resolution = resolutionResult.get();

		try
		{
			Path lintReportPath = reportPath != null ? reportPath.toPath()
					: projectBuildDirectory.toPath().resolve("dsf-linter-report");

			Files.createDirectories(lintReportPath);

			DsfLinter.Config config = new DsfLinter.Config(resolution.resolvedPath().toAbsolutePath(),
					lintReportPath.toAbsolutePath(), generateHtmlReport, generateJsonReport, failOnErrors, logger);

			OverallLinterResult result = new DsfLinter(config).lint();

			if (!result.success())
			{
				String msg = "DSF Linter: " + result.getTotalErrors() + " error(s), " + result.getPluginWarnings()
						+ " warning(s)";

				if (failOnErrors)
				{
					throw new MojoFailureException(msg);
				}

				getLog().warn(msg);
			}
			else
			{
				getLog().info("DSF Linter: passed - no errors found");
			}
		}
		catch (IOException e)
		{
			throw new MojoExecutionException("DSF Linter failed", e);
		}
		finally
		{
			if (resolution.requiresCleanup())
			{
				resolver.cleanup(resolution);
			}
		}
	}
}
