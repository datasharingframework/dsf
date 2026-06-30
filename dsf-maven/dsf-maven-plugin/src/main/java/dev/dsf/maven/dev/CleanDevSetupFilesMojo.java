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

import java.io.File;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import dev.dsf.maven.exception.RuntimeIOException;

/**
 * Cleans up certificate files for a local DSF development setup.
 * <p>
 * This goal deletes all generated certificate files (client, server, CA chain) from the configured target directories.
 * <p>
 * Use <code>-Ddsf.includeCertDir=true</code> and/or <code>-Ddsf.includeKeyDir=true</code> to also delete the plugin
 * cache files.
 */
@Mojo(name = "clean-dev-setup-files", defaultPhase = LifecyclePhase.CLEAN, requiresDependencyResolution = ResolutionScope.NONE, threadSafe = true, aggregator = true)
public class CleanDevSetupFilesMojo extends AbstractMojo
{
	/**
	 * The base directory of the project.
	 */
	@Parameter(defaultValue = "${project.basedir}", readonly = true, required = true)
	private File projectBasedir;

	/**
	 * The directory to write the generated certificate files to.
	 */
	@Parameter(required = true, property = "dsf.certDir", defaultValue = "cert")
	private File certDir;

	/**
	 * The directory to write the generated key files to.
	 */
	@Parameter(required = true, property = "dsf.keyDir", defaultValue = "key")
	private File keyDir;

	/**
	 * The certificates to generate. See <a href="index.html">usage</a> for details.
	 */
	@Parameter
	private List<Cert> certs;

	/**
	 * The key-pairs to generate. See <a href="index.html">usage</a> for details.
	 */
	@Parameter
	private List<Key> keys;

	/**
	 * The root CA configuration.
	 */
	@Parameter
	private RootCa rootCa;

	/**
	 * The issuing CA configuration.
	 */
	@Parameter
	private IssuingCa issuingCa;

	/**
	 * The CA chain configuration.
	 */
	@Parameter
	private CaChain caChain;

	/**
	 * The templates to apply. See <a href="index.html">usage</a> for details.
	 */
	@Parameter
	private List<Template> templates;

	/**
	 * Whether to delete the cert directory with its contents as well.
	 */
	@Parameter(required = true, property = "dsf.includeCertDir", defaultValue = "false")
	private boolean includeCertDir;

	/**
	 * Whether to delete the key directory with its contents as well.
	 */
	@Parameter(required = true, property = "dsf.includeKeyDir", defaultValue = "false")
	private boolean includeKeyDir;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		getLog().debug("projectBasedir: " + projectBasedir);
		getLog().debug("certDir: " + certDir);
		getLog().debug("keyDir: " + keyDir);
		getLog().debug("certs: " + certs);
		getLog().debug("keys: " + keys);
		getLog().debug("rootCa: " + rootCa);
		getLog().debug("issuingCa: " + issuingCa);
		getLog().debug("caChain: " + caChain);
		getLog().debug("templates: " + templates);
		getLog().debug("includeCertDir: " + includeCertDir);
		getLog().debug("includeKeyDir: " + includeKeyDir);

		FileRemover fileRemover = new FileRemover(projectBasedir.toPath(), certDir.toPath(), keyDir.toPath());

		try
		{
			fileRemover.deleteCerts(certs);
			fileRemover.delete(rootCa);
			fileRemover.delete(issuingCa);
			fileRemover.delete(caChain);
			fileRemover.deleteTemplates(templates);
			fileRemover.deleteKeys(keys);

			if (includeCertDir)
				fileRemover.deleteFilesInCertDir(certs);
			if (includeKeyDir)
				fileRemover.deleteFilesInKeyDir(keys);
		}
		catch (RuntimeIOException e)
		{
			throw new MojoFailureException(e);
		}
	}
}
