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
package dev.dsf.maven.ca;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Generates default CA files for DSF docker images.
 * <p>
 * This goal creates all required default CA files (client issuing CAs, client CA chains, server root CAs) and copies
 * them to the configured target directory.
 */
@Mojo(name = "generate-default-ca-files", defaultPhase = LifecyclePhase.PREPARE_PACKAGE, requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true)
public class GenerateDefaultCaFilesMojo extends AbstractMojo
{
	@Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
	private File projectBasedir;

	@Parameter(property = "dsf.certFolder", required = true)
	private File certFolder;

	@Parameter(property = "dsf.clientOnlyCaCommonNames", required = true)
	private List<String> clientOnlyCaCommonNames;

	@Parameter(property = "dsf.serverOnlyCaCommonNames", required = true)
	private List<String> serverOnlyCaCommonNames;

	@Parameter(property = "dsf.clientIssuingCas", required = true)
	private List<File> clientIssuingCas;

	@Parameter(property = "dsf.clientCaChains", required = true)
	private List<File> clientCaChains;

	@Parameter(property = "dsf.serverRootCas", required = true)
	private List<File> serverRootCas;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		getLog().debug("projectBasedir: " + projectBasedir);
		getLog().debug("certFolder: " + certFolder);
		getLog().debug("clientOnlyCaCommonNames: " + clientOnlyCaCommonNames);
		getLog().debug("serverOnlyCaCommonNames: " + serverOnlyCaCommonNames);
		getLog().debug("clientIssuingCas: " + clientIssuingCas);
		getLog().debug("clientCaChains: " + clientCaChains);
		getLog().debug("serverRootCas: " + serverRootCas);

		try
		{
			new DefaultCaFilesGenerator(projectBasedir.toPath(), certFolder.toPath(), clientOnlyCaCommonNames,
					serverOnlyCaCommonNames).createFiles(clientIssuingCas.stream().map(File::toPath),
							clientCaChains.stream().map(File::toPath), serverRootCas.stream().map(File::toPath));
		}
		catch (IOException e)
		{
			throw new MojoFailureException(e);
		}
	}
}
