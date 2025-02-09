package dev.rdh.amnesia.ij

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoFilter
import com.intellij.compiler.CompilerConfiguration
import com.intellij.openapi.module.ModuleUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiJavaFile

class AmnesiaHighlightFilter : HighlightInfoFilter {
    private val jarRegex = Regex("amnesia-.*\\.jar")
    override fun accept(
        highlight: HighlightInfo,
        file: PsiFile?
    ): Boolean {
        if (file !is PsiJavaFile || highlight.description == null) {
            return true
        }

        val module = ModuleUtil.findModuleForPsiElement(file) ?: return true
        val javacConfig = CompilerConfiguration.getInstance(module.project)
        val apConfig = javacConfig.getAnnotationProcessingConfiguration(module)
        if (!apConfig.isEnabled) return true

        if (!jarRegex.containsMatchIn(apConfig.processorPath)) {
            return true
        }

        return listOf("Unhandled exception", "未处理的异常", "未处理 异常")
            .none { it in highlight.description }
    }

}