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

public class Amnesia extends Log.DiagnosticHandler implements Plugin {
	@Override
	public String getName() {
		return "amnesia";
	}

	private boolean warn = false;
	private Field diagnostic_type;

	@Override
	public void init(JavacTask task, String... args) {
		for(String arg : args) {
			if(arg.equals("-warn")) {
				warn = true;
			} else {
				throw new IllegalArgumentException("Unknown argument: " + arg);
			}
		}

		Context context = ((BasicJavacTask) task).getContext();
		Log log = Log.instance(context);
		this.install(log);

		try {
			(diagnostic_type = JCDiagnostic.class.getDeclaredField("type")).setAccessible(true);
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}

	@Override
	public void report(JCDiagnostic diag) {
		if(diag.getCode().startsWith("compiler.err.unreported.exception")) {
			if(warn) {
				try {
					diagnostic_type.set(diag, DiagnosticType.WARNING);
				} catch (Throwable t) {
					throw new RuntimeException(t);
				}
				prev.report(diag);
			}
		} else if(!diag.getCode().equals("compiler.err.except.never.thrown.in.try")) {
			prev.report(diag);
		}
	}

	static {
		try {
			int vmVersion = Integer.parseInt(System.getProperty("java.specification.version").split("\\.")[1]);
			if(vmVersion > 8) {
				//noinspection JavaReflectionMemberAccess
				Method getModule = Class.class.getDeclaredMethod("getModule");
				Object module = getModule.invoke(JavacTask.class);

				Class<?> moduleClass = module.getClass();
				if(!moduleClass.getName().equals("java.lang.Module")) {
					throw new IllegalArgumentException("Not a module: " + moduleClass);
				}

				Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
				unsafeField.setAccessible(true);
				Unsafe u = (Unsafe) unsafeField.get(null);

				Field f = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
				MethodHandles.Lookup lookup = (MethodHandles.Lookup) u.getObject(u.staticFieldBase(f), u.staticFieldOffset(f));

				MethodHandle implAddOpens = lookup.findVirtual(moduleClass, "implAddOpens", MethodType.methodType(void.class, String.class));

				@SuppressWarnings("unchecked")
				Set<String> packages = (Set<String>) moduleClass.getDeclaredMethod("getPackages").invoke(module);

				for(String pn : packages) {
					implAddOpens.invoke(module, pn);
				}
			}
		} catch (Throwable t) {
			throw new RuntimeException(t);
		}
	}
}