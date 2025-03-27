package dev.dsf.bpe.test;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Stream;

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

	@FunctionalInterface
	public interface RunnableWithException
	{
		void run() throws Exception;
	}

	public static final void execute(Object testClass, Consumer<String> addTestSucceededToStartTask,
			Consumer<String> addTestFailedToStartTask, Runnable updateStartTask, Object testMethodArg0,
			Object testMethodArg1, Object... testMethodArgs)
	{
		Arrays.stream(testClass.getClass().getDeclaredMethods())
				.filter(m -> m.getAnnotationsByType(PluginTest.class).length == 1)
				.filter(m -> m.getParameterCount() <= testMethodArgs.length).forEach(m ->
				{
					try
					{
						logger.info("Executing test method {}.{} ...", testClass.getClass().getName(), m.getName());

						Class<?>[] parameterTypes = m.getParameterTypes();
						Object[] values = Arrays.stream(m.getParameterTypes()).flatMap(parameterType -> Stream
								.concat(Stream.of(testMethodArg0, testMethodArg1), Arrays.stream(testMethodArgs))
								.filter(value -> parameterType.isAssignableFrom(value.getClass())).findFirst().stream())
								.toArray();

						if (values.length != parameterTypes.length)
							throw new IllegalArgumentException(
									"One or more parameters of test method '" + m.getName() + "' not supported");

						m.invoke(testClass, values);

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

	public static void expectNotNull(Object actual)
	{
		if (actual == null)
			throw new TestAssertException("Object is null, expected not null");
	}

	public static void expectNull(Object actual)
	{
		if (actual != null)
			throw new TestAssertException(actual.getClass().getSimpleName() + " is not null, expected null");
	}

	public static void expectTrue(boolean actual)
	{
		if (!actual)
			throw new TestAssertException("Boolean value is false, expected true");
	}

	public static void expectFalse(boolean actual)
	{
		if (actual)
			throw new TestAssertException("Boolean value is true, expected false");
	}

	public static void expectSame(Object expected, Object actual)
	{
		if (!Objects.equals(expected, actual))
			throw createTestAssertExceptionNotSame("Object", Objects.toString(expected), Objects.toString(actual));
	}

	public static void expectSame(byte expected, byte actual)
	{
		if (expected != actual)
			throw createTestAssertExceptionNotSame("byte", Objects.toString(expected), Objects.toString(actual));
	}

	public static void expectSame(int expected, int actual)
	{
		if (expected != actual)
			throw createTestAssertExceptionNotSame("int", Objects.toString(expected), Objects.toString(actual));
	}

	public static void expectSame(long expected, long actual)
	{
		if (expected != actual)
			throw createTestAssertExceptionNotSame("long", Objects.toString(expected), Objects.toString(actual));
	}

	public static void expectSame(float expected, float actual)
	{
		if (expected != actual)
			throw createTestAssertExceptionNotSame("float", Objects.toString(expected), Objects.toString(actual));
	}

	public static void expectSame(double expected, double actual)
	{
		if (expected != actual)
			throw createTestAssertExceptionNotSame("double", Objects.toString(expected), Objects.toString(actual));
	}

	public static void expectSame(char expected, char actual)
	{
		if (expected != actual)
			throw createTestAssertExceptionNotSame("char", Objects.toString(expected), Objects.toString(actual));
	}

	public static void expectSame(byte[] expected, byte[] actual)
	{
		if (!Arrays.equals(expected, actual))
			throw createTestAssertExceptionNotSame("byte[]", Arrays.toString(expected), Arrays.toString(actual));
	}

	public static void expectSame(int[] expected, int[] actual)
	{
		if (!Arrays.equals(expected, actual))
			throw createTestAssertExceptionNotSame("int[]", Arrays.toString(expected), Arrays.toString(actual));
	}

	public static void expectSame(long[] expected, long[] actual)
	{
		if (!Arrays.equals(expected, actual))
			throw createTestAssertExceptionNotSame("long[]", Arrays.toString(expected), Arrays.toString(actual));
	}

	public static void expectSame(float[] expected, float[] actual)
	{
		if (!Arrays.equals(expected, actual))
			throw createTestAssertExceptionNotSame("float[]", Arrays.toString(expected), Arrays.toString(actual));
	}

	public static void expectSame(double[] expected, double[] actual)
	{
		if (!Arrays.equals(expected, actual))
			throw createTestAssertExceptionNotSame("double[]", Arrays.toString(expected), Arrays.toString(actual));
	}

	public static void expectSame(char[] expected, char[] actual)
	{
		if (!Arrays.equals(expected, actual))
			throw createTestAssertExceptionNotSame("char[]", Arrays.toString(expected), Arrays.toString(actual));
	}

	private static TestAssertException createTestAssertExceptionNotSame(String type, String expected, String actual)
	{
		throw new TestAssertException(
				"Tested " + type + " is not same as expected [expected: " + expected + ", actual: " + actual + "]");
	}

	public static void expectException(Class<?> expectedException, RunnableWithException run)
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
				throw new TestAssertException("Expected " + expectedException.getName() + " but caught "
						+ e.getClass().getName()
						+ (e.getMessage() != null && !e.getMessage().isBlank() ? " (" + e.getMessage() + ")" : ""));
		}
	}
}
