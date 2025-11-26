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
package dev.dsf.common.auth;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;

import dev.dsf.common.auth.conf.RoleConfigReader;

public class RoleConfigReaderTest
{
	@Test(expected = NullPointerException.class)
	public void testReadNullString() throws Exception
	{
		new RoleConfigReader().read((String) null, _ -> null, _ -> null);
	}

	@Test
	public void testReadEmptyString() throws Exception
	{
		new RoleConfigReader().read("", _ -> null, _ -> null);
	}

	@Test(expected = NullPointerException.class)
	public void testReadNullInputStream() throws Exception
	{
		new RoleConfigReader().read((InputStream) null, _ -> null, _ -> null);
	}

	@Test
	public void testReadEmptyInputStream() throws Exception
	{
		new RoleConfigReader().read(new ByteArrayInputStream(new byte[0]), _ -> null, _ -> null);
	}
}
