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
package dev.dsf.bpe.test.service;

import static dev.dsf.bpe.test.PluginTestExecutor.expectNotNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectNull;
import static dev.dsf.bpe.test.PluginTestExecutor.expectSame;

import java.util.List;
import java.util.Map;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Bundle.BundleEntryComponent;
import org.hl7.fhir.r4.model.IdType;

import dev.dsf.bpe.test.AbstractTest;
import dev.dsf.bpe.test.PluginTest;
import dev.dsf.bpe.v2.ProcessPluginApi;
import dev.dsf.bpe.v2.activity.ServiceTask;
import dev.dsf.bpe.v2.client.dsf.BinaryInputStream;
import dev.dsf.bpe.v2.client.dsf.DsfClient;
import dev.dsf.bpe.v2.error.ErrorBoundaryEvent;
import dev.dsf.bpe.v2.variables.Variables;
import jakarta.ws.rs.core.MediaType;

public class DsfClientTest extends AbstractTest implements ServiceTask
{
	@Override
	public void execute(ProcessPluginApi api, Variables variables) throws ErrorBoundaryEvent, Exception
	{
		DsfClient localDsfClient = api.getDsfClientProvider().getLocalDsfClient();

		Bundle search = localDsfClient.search(Binary.class, Map.of("_count", List.of("1")));
		IdType testBinaryId = search.getEntry().stream().filter(BundleEntryComponent::hasResource)
				.map(BundleEntryComponent::getResource).filter(r -> r instanceof Binary).map(r -> r.getIdElement())
				.findFirst().get();

		executeTests(api, variables, localDsfClient, testBinaryId);
	}

	@PluginTest
	public void downloadFull(DsfClient localDsfClient, IdType testBinaryId) throws Exception
	{
		try (BinaryInputStream binary = localDsfClient.readBinary(testBinaryId.getIdPart(),
				MediaType.APPLICATION_OCTET_STREAM_TYPE))
		{
			expectNotNull(binary);
			expectSame(10, binary.getContentLength());
			expectNull(binary.getRange());

			byte[] allBytes = binary.readAllBytes();
			expectSame(10, allBytes.length);
			expectSame(0, allBytes[0]);
			expectSame(1, allBytes[1]);
			expectSame(2, allBytes[2]);
			expectSame(3, allBytes[3]);
			expectSame(4, allBytes[4]);
			expectSame(5, allBytes[5]);
			expectSame(6, allBytes[6]);
			expectSame(7, allBytes[7]);
			expectSame(8, allBytes[8]);
			expectSame(9, allBytes[9]);
		}
	}

	@PluginTest
	public void downloadFullVersion(DsfClient localDsfClient, IdType testBinaryId) throws Exception
	{
		try (BinaryInputStream binary = localDsfClient.readBinary(testBinaryId.getIdPart(),
				testBinaryId.getVersionIdPart(), MediaType.APPLICATION_OCTET_STREAM_TYPE))
		{
			expectNotNull(binary);
			expectSame(10, binary.getContentLength());
			expectNull(binary.getRange());

			byte[] allBytes = binary.readAllBytes();
			expectSame(10, allBytes.length);
			expectSame(0, allBytes[0]);
			expectSame(1, allBytes[1]);
			expectSame(2, allBytes[2]);
			expectSame(3, allBytes[3]);
			expectSame(4, allBytes[4]);
			expectSame(5, allBytes[5]);
			expectSame(6, allBytes[6]);
			expectSame(7, allBytes[7]);
			expectSame(8, allBytes[8]);
			expectSame(9, allBytes[9]);
		}
	}

	@PluginTest
	public void downloadRange(DsfClient localDsfClient, IdType testBinaryId) throws Exception
	{
		try (BinaryInputStream binary = localDsfClient.readBinary(testBinaryId.getIdPart(),
				MediaType.APPLICATION_OCTET_STREAM_TYPE, 0L, 1L))
		{
			expectNotNull(binary);
			expectSame(2, binary.getContentLength());
			expectNotNull(binary.getRange());
			expectSame(0, binary.getRange().start());
			expectSame(1, binary.getRange().end());
			expectSame(10, binary.getRange().size());

			byte[] allBytes = binary.readAllBytes();
			expectSame(2, allBytes.length);
			expectSame(0, allBytes[0]);
			expectSame(1, allBytes[1]);
		}
	}

	@PluginTest
	public void downloadRangeVerion(DsfClient localDsfClient, IdType testBinaryId) throws Exception
	{
		try (BinaryInputStream binary = localDsfClient.readBinary(testBinaryId.getIdPart(),
				testBinaryId.getVersionIdPart(), MediaType.APPLICATION_OCTET_STREAM_TYPE, 0L, 1L))
		{
			expectNotNull(binary);
			expectSame(2, binary.getContentLength());
			expectNotNull(binary.getRange());
			expectSame(0, binary.getRange().start());
			expectSame(1, binary.getRange().end());
			expectSame(10, binary.getRange().size());

			byte[] allBytes = binary.readAllBytes();
			expectSame(2, allBytes.length);
			expectSame(0, allBytes[0]);
			expectSame(1, allBytes[1]);
		}
	}
}
