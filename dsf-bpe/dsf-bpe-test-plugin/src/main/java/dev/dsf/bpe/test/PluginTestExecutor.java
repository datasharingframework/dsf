package dev.dsf.bpe.test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PluginTestExecutor
{
	private static final Logger logger = LoggerFactory.getLogger(PluginTestExecutor.class);

	private static final class TestAssertException extends RuntimeException
	{
		private static final long serialVersionUID = 1L;

		public TestAssertException(String message)
		{
			super(message);
		}
	}

	public static final void execute(Object testClass, Consumer<String> addTestSucceededToStartTask,
			Consumer<String> addTestFailedToStartTask, Runnable updateStartTask)
	{
		Arrays.stream(testClass.getClass().getDeclaredMethods())
				.filter(m -> m.getAnnotationsByType(PluginTest.class).length == 1)
				.filter(m -> m.getParameterCount() == 0).forEach(m ->
				{
					try
					{
						logger.info("Executing test method {}.{} ...", testClass.getClass().getName(), m.getName());
						m.invoke(testClass);
						logger.info("Executing test method {}.{} [succeeded]", testClass.getClass().getName(),
								m.getName());

						addTestSucceededToStartTask.accept(testClass.getClass().getName() + "." + m.getName());
					}
					catch (InvocationTargetException e)
					{
						if (e.getCause() instanceof TestAssertException t)
						{
							String location = t.getStackTrace() != null && t.getStackTrace().length > 1
									? (t.getStackTrace()[1].getClassName() + ":" + t.getStackTrace()[1].getLineNumber())
									: "?";
							logger.warn("Executing test method {}.{} [failed] - {} at {}",
									testClass.getClass().getName(), m.getName(), t.getMessage(), location);
						}
						else
							logger.error("Executing test method {}.{} [error] - {}: {}", testClass.getClass().getName(),
									m.getName(), e.getClass().getName(), e.getMessage(), e);

						addTestFailedToStartTask.accept(testClass.getClass().getName() + "." + m.getName());
					}
					catch (Exception e)
					{
						logger.error("Executing test method {}.{} [error] - {}: {}", testClass.getClass().getName(),
								m.getName(), e.getClass().getName(), e.getMessage(), e);

						addTestFailedToStartTask.accept(testClass.getClass().getName() + "." + m.getName());
					}
				});

		updateStartTask.run();
	}

	public static void isNotNull(Object test)
	{
		if (test == null)
			throw new TestAssertException("Object is null, expected not null");
	}

	public static void isNull(Object test)
	{
		if (test != null)
			throw new TestAssertException("Object is not null, expected null");
	}

	public static void isTrue(boolean test)
	{
		if (!test)
			throw new TestAssertException("Boolean value is false, expected true");
	}

	public static void isFalse(boolean test)
	{
		if (test)
			throw new TestAssertException("Boolean value is true, expected false");
	}

	public static void isSame(Object expected, Object test)
	{
		if (!Objects.equals(expected, test))
			throw new TestAssertException("Tested object is not same as expected [expected: "
					+ Objects.toString(expected) + ", actual: " + Objects.toString(test) + "]");
	}

	public static void isSame(byte expected, byte test)
	{
		if (expected != test)
			throw new TestAssertException("Tested object is not same as expected [expected: "
					+ Objects.toString(expected) + ", actual: " + Objects.toString(test) + "]");
	}

	public static void isSame(int expected, int test)
	{
		if (expected != test)
			throw new TestAssertException("Tested object is not same as expected [expected: "
					+ Objects.toString(expected) + ", actual: " + Objects.toString(test) + "]");
	}

	public static void isSame(long expected, long test)
	{
		if (expected != test)
			throw new TestAssertException("Tested object is not same as expected [expected: "
					+ Objects.toString(expected) + ", actual: " + Objects.toString(test) + "]");
	}

	public static void isSame(float expected, float test)
	{
		if (expected != test)
			throw new TestAssertException("Tested object is not same as expected [expected: "
					+ Objects.toString(expected) + ", actual: " + Objects.toString(test) + "]");
	}

	public static void isSame(double expected, double test)
	{
		if (expected != test)
			throw new TestAssertException("Tested object is not same as expected [expected: "
					+ Objects.toString(expected) + ", actual: " + Objects.toString(test) + "]");
	}

	public static void isSame(char expected, char test)
	{
		if (expected != test)
			throw new TestAssertException("Tested object is not same as expected [expected: "
					+ Objects.toString(expected) + ", actual: " + Objects.toString(test) + "]");
	}

	public static void isSame(byte[] expected, byte[] test)
	{
		if (!Arrays.equals(expected, test))
			throw new TestAssertException("Tested object is not same as expected [expected: "
					+ Arrays.toString(expected) + ", actual: " + Arrays.toString(test) + "]");
	}

	public static void isSame(int[] expected, int[] test)
	{
		if (!Arrays.equals(expected, test))
			throw new TestAssertException("Tested object is not same as expected [expected: "
					+ Arrays.toString(expected) + ", actual: " + Arrays.toString(test) + "]");
	}

	public static void isSame(long[] expected, long[] test)
	{
		if (!Arrays.equals(expected, test))
			throw new TestAssertException("Tested object is not same as expected [expected: "
					+ Arrays.toString(expected) + ", actual: " + Arrays.toString(test) + "]");
	}

	public static void isSame(float[] expected, float[] test)
	{
		if (!Arrays.equals(expected, test))
			throw new TestAssertException("Tested object is not same as expected [expected: "
					+ Arrays.toString(expected) + ", actual: " + Arrays.toString(test) + "]");
	}

	public static void isSame(double[] expected, double[] test)
	{
		if (!Arrays.equals(expected, test))
			throw new TestAssertException("Tested object is not same as expected [expected: "
					+ Arrays.toString(expected) + ", actual: " + Arrays.toString(test) + "]");
	}

	public static void isSame(char[] expected, char[] test)
	{
		if (!Arrays.equals(expected, test))
			throw new TestAssertException("Tested object is not same as expected [expected: "
					+ Arrays.toString(expected) + ", actual: " + Arrays.toString(test) + "]");
	}

	public static void expectException(Class<?> expectedException, Runnable run)
	{
		Objects.requireNonNull(expectedException, "expectedException");
		Objects.requireNonNull(run, "run");

		try
		{
			run.run();
		}
		catch (Exception e)
		{
			if (!expectedException.isInstance(e))
				throw new TestAssertException(
						"Expected " + expectedException.getName() + " but caught " + e.getClass().getName());
		}
	}
}
