package dev.rdh.amnesia.ij;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter;
import com.intellij.compiler.CompilerConfiguration;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.model.java.compiler.AnnotationProcessingConfiguration;

import java.lang.reflect.Field;
import java.util.regex.Pattern;

@SuppressWarnings("StaticInitializationInExtensions") // this is really cheap
public class AmnesiaHighlightFilter implements HighlightInfoFilter {
	private static final Pattern JAR_PATTERN = Pattern.compile("amnesia-.*\\.jar");

	// TODO: detect based on presence of `-warn`/`-note` in javac args
	private static final HighlightSeverity severity = HighlightSeverity.WARNING;
	private static final Field severityField;

	static {
		Field f = HighlightInfo.class.getDeclaredField("severity");
		f.trySetAccessible();
		severityField = f;
	}

	@Override
	public boolean accept(@NotNull HighlightInfo highlightInfo, @Nullable PsiFile file) {
		String description = highlightInfo.getDescription();
		if (!(file instanceof PsiJavaFile) || description == null) return true;

		Module module = ModuleUtil.findModuleForPsiElement(file);
		if (module == null) return true;

		CompilerConfiguration javacConfig = CompilerConfiguration.getInstance(module.getProject());
		if (javacConfig == null) return true;

		AnnotationProcessingConfiguration apConfig = javacConfig.getAnnotationProcessingConfiguration(module);
		if (!apConfig.isEnabled()) return true;

		String processorPath = apConfig.getProcessorPath();

		if (!JAR_PATTERN.matcher(processorPath).find()) return true;

		String[] targets = {"Unhandled exception", "未处理的异常", "未处理 异常"};
		for (String target : targets) {
			if (description.contains(target)) {
				if(severity != null) {
					severityField.set(highlightInfo, severity);
					break;
				} else {
					return false;
				}
			}
		}

		return true;
	}
}
