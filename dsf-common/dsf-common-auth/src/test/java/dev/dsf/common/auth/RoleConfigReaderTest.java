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
