package dev.dsf.bpe.v1.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class ProcessPluginClassLoader extends URLClassLoader
{
	private static final String FROM_PACKAGE = "org/camunda/bpm";
	private static final String TO_PACKAGE = "org/operaton/bpm";

	public ProcessPluginClassLoader(String name, URL[] urls, ClassLoader parent)
	{
		super(name, urls, parent);
	}

	@Override
	protected Class<?> findClass(String name) throws ClassNotFoundException
	{
		try (InputStream is = getResourceAsStream(name.replace('.', '/') + ".class"))
		{
			if (is == null)
				throw new ClassNotFoundException(name);

			ClassReader reader = new ClassReader(is);
			ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

			ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9, writer)
			{
				@Override
				public void visit(int version, int access, String name, String signature, String superName,
						String[] interfaces)
				{
					super.visit(version, access, name, signature, replace(superName), replaceAll(interfaces));
				}

				@Override
				public FieldVisitor visitField(int access, String name, String descriptor, String signature,
						Object value)
				{
					return super.visitField(access, name, replace(descriptor), replace(signature), value);
				}

				@Override
				public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
						String[] exceptions)
				{
					MethodVisitor mv = super.visitMethod(access, name, replace(descriptor), replace(signature),
							replaceAll(exceptions));

					return new MethodVisitor(Opcodes.ASM9, mv)
					{
						@Override
						public void visitTypeInsn(int opcode, String type)
						{
							super.visitTypeInsn(opcode, replace(type));
						}

						@Override
						public void visitFieldInsn(int opcode, String owner, String name, String descriptor)
						{
							super.visitFieldInsn(opcode, replace(owner), name, replace(descriptor));
						}

						@Override
						public void visitMethodInsn(int opcode, String owner, String name, String descriptor,
								boolean isInterface)
						{
							super.visitMethodInsn(opcode, replace(owner), name, replace(descriptor), isInterface);
						}

						@Override
						public void visitLdcInsn(Object value)
						{
							if (value instanceof Type)
								value = Type.getType(replace(((Type) value).getDescriptor()));
							else if (value instanceof String && ((String) value).contains(FROM_PACKAGE))
								value = ((String) value).replace(FROM_PACKAGE, TO_PACKAGE);

							super.visitLdcInsn(value);
						}

						@Override
						public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle,
								Object... bootstrapMethodArguments)
						{
							super.visitInvokeDynamicInsn(name, replace(descriptor), bootstrapMethodHandle,
									replaceMethodArguments(bootstrapMethodArguments));
						}
					};
				}
			};

			reader.accept(visitor, ClassReader.EXPAND_FRAMES);

			byte[] modified = writer.toByteArray();

			return defineClass(name, modified, 0, modified.length);
		}
		catch (IOException e)
		{
			throw new ClassNotFoundException("Failed to load class: " + name, e);
		}
	}

	private String replace(String name)
	{
		return name == null ? null : name.replace(FROM_PACKAGE, TO_PACKAGE);
	}

	private String[] replaceAll(String[] names)
	{
		if (names == null)
			return null;

		String[] result = new String[names.length];

		for (int i = 0; i < names.length; i++)
			result[i] = replace(names[i]);

		return result;
	}

	private Object[] replaceMethodArguments(Object... args)
	{
		Object[] newArgs = new Object[args.length];

		for (int i = 0; i < args.length; i++)
		{
			Object arg = args[i];
			if (arg instanceof Type t)
				newArgs[i] = Type.getType(replace(t.getDescriptor()));
			else if (arg instanceof Handle h)
				newArgs[i] = new Handle(h.getTag(), h.getOwner(), h.getName(), replace(h.getDesc()), h.isInterface());
			else
				newArgs[i] = arg;
		}

		return newArgs;
	}
}
