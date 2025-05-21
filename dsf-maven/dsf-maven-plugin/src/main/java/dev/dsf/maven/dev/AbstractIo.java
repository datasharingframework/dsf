package dev.dsf.maven.dev;

import java.io.IOException;

import dev.dsf.maven.exception.RuntimeIOException;

public abstract class AbstractIo
{
	protected static interface RunnableWithIoException
	{
		void run() throws IOException;
	}

	protected final void toRuntimeException(RunnableWithIoException runnable)
	{
		try
		{
			runnable.run();
		}
		catch (IOException e)
		{
			throw new RuntimeIOException(e);
		}
	}

}
