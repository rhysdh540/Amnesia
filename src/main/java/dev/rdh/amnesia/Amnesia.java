package dev.rdh.amnesia;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.JCDiagnostic;
import com.sun.tools.javac.util.JCDiagnostic.DiagnosticType;
import com.sun.tools.javac.util.Log;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Amnesia extends Log.DiagnosticHandler implements Plugin {
	@Override
	public String getName() {
		return "amnesia";
	}

	private DiagnosticType type = null;
	private static final MethodHandle setter;

	@Override
	public void init(JavacTask task, String... args) {
		for(String arg : args) {
			if(arg.equals("-warn")) {
				type = DiagnosticType.WARNING;
			} else if(arg.equals("-note")) {
				type = DiagnosticType.NOTE;
			} else {
				throw new IllegalArgumentException("Unknown argument: " + arg);
			}
		}

		Context context = ((BasicJavacTask) task).getContext();
		this.install(Log.instance(context));
	}

	@Override
	public void report(JCDiagnostic diag) {
		String message = diag.getCode();
		if(message.startsWith("compiler.err.unreported.exception") || message.equals("compiler.err.except.never.thrown.in.try")) {
			if(type != null) {
				setter.invoke(diag, type);
				prev.report(diag);
			}
		} else {
			prev.report(diag);
		}
	}

	static {
		Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		Unsafe u = (Unsafe) unsafeField.get(null);

		Field f = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
		MethodHandles.Lookup lookup = (MethodHandles.Lookup) u.getObject(u.staticFieldBase(f), u.staticFieldOffset(f));

		String version = System.getProperty("java.specification.version");
		Matcher m = Pattern.compile("^(?:1\\.)?(\\d+).*").matcher(version);
		if(!m.find()) {
			throw new IllegalArgumentException("Invalid version: " + version);
		}

		int vmVersion = Integer.parseInt(m.group(1));
		if(vmVersion > 8) {
			//noinspection JavaReflectionMemberAccess
			Method getModule = Class.class.getDeclaredMethod("getModule");
			Object module = getModule.invoke(JavacTask.class);

			Class<?> moduleClass = module.getClass();
			if(!moduleClass.getName().equals("java.lang.Module")) {
				throw new IllegalArgumentException("Not a module: " + moduleClass);
			}

			MethodHandle implAddOpens = lookup.findVirtual(moduleClass, "implAddOpens", MethodType.methodType(void.class, String.class));

			@SuppressWarnings("unchecked")
			Set<String> packages = (Set<String>) moduleClass.getDeclaredMethod("getPackages").invoke(module);

			for(String pn : packages) {
				implAddOpens.invoke(module, pn);
			}
		}

		setter = lookup.findSetter(JCDiagnostic.class, "type", DiagnosticType.class);
	}
}