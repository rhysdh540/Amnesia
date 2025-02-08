import java.io.StringWriter
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.2.1"
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        bundledPlugin("com.intellij.java")

        pluginVerifier()
        zipSigner()
    }
}

tasks.processResources {
    filesMatching(listOf("**/*.xml", "**/*.svg", "**/*.html"), MinifyXmlAction)
}

object MinifyXmlAction : Action<FileCopyDetails> {
    private val xslt: String = """
            <?xml version="1.0" encoding="UTF-8"?>
            <xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
                <!-- remove whitespace -->
                <xsl:output indent="no" omit-xml-declaration="yes"/>
                <xsl:strip-space elements="*"/>
                
                <!-- copy all nodes and attributes -->
                <xsl:template match="@*|node()">
                    <xsl:copy>
                        <xsl:apply-templates select="@*|node()"/>
                    </xsl:copy>
                </xsl:template>
                
                <!-- remove comments and whitespace -->
                <xsl:template match="comment()"/>
                
                <xsl:template match="text()">
                    <xsl:if test="normalize-space() != ''">
                        <xsl:value-of select="."/>
                    </xsl:if>
                </xsl:template>
            </xsl:stylesheet>
        """.trimIndent()

    override fun execute(details: FileCopyDetails) {
        val writer = StringWriter()
        val transformer = TransformerFactory.newInstance().newTransformer(StreamSource(xslt.reader()))
        transformer.transform(
            StreamSource(details.file.reader()),
            StreamResult(writer)
        )

        val output = writer.toString()

        var changed = false
        details.filter(object : groovy.lang.Closure<String?>(this) {
            @Suppress("unused")
            fun doCall(line: String): String? {
                return if (!changed) {
                    changed = true
                    output
                } else {
                    null
                }
            }
        })
    }
}