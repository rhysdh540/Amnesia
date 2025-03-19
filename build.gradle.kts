import org.taumc.gradle.compression.DeflateAlgorithm
import org.taumc.gradle.compression.task.AdvzipTask
import org.taumc.gradle.util.capitalized

plugins {
    id("java")
    id("idea")
    id("maven-publish")
    id("org.taumc.gradle.compression") version("0.3.27")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "idea")
    apply(plugin = "org.taumc.gradle.compression")

    group = "dev.rdh"
    version = "1.1.0"
    base.archivesName = "amnesia${if (project != rootProject) "-${project.name}" else ""}"

    idea {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-g:none")
    }

    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://maven.taumc.org/releases")
    }

    dependencies {
        annotationProcessor("dev.rdh:amnesia:1.1.0")
    }

    tasks.jar {
        isReproducibleFileOrder = true
        isPreserveFileTimestamps = false

        manifest {
            attributes["Implementation-Title"] = "Amnesia${if (project != rootProject) " ${project.name.capitalized()}" else ""}"
            attributes["Implementation-Version"] = version
        }
    }
}

val java8 = javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(8)) }.get()

dependencies {
    implementation(files("${java8.metadata.installationPath}/lib/tools.jar"))
}

tasks.compileJava {
    options.compilerArgs.add("-Xplugin:amnesia")
}

val compressJar = tau.compression.compress<AdvzipTask>(tasks.jar) {
    level = DeflateAlgorithm.INSANE
    iterations = 1000
}

publishing {
    repositories {
        maven {
            name = "TauMC"
            url = uri("https://maven.taumc.org/releases")
            credentials {
                username = System.getenv("TAUMC_MAVEN_USERNAME")
                password = System.getenv("TAUMC_MAVEN_SECRET")
            }
        }
    }

    publications {
        create<MavenPublication>("mavenJava") {
            artifact(compressJar)
            artifactId = base.archivesName.get()
        }
    }
}