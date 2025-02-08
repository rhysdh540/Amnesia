import org.taumc.gradle.compression.DeflateAlgorithm
import org.taumc.gradle.compression.task.AdvzipTask

plugins {
    id("java")
    id("idea")
    id("maven-publish")
    id("org.taumc.gradle.compression") version("0.1.3")
}

group = "dev.rdh"
version = "1.1"
base.archivesName = "amnesia"

repositories {
    mavenCentral()
    maven("https://maven.taumc.org/releases")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

val java8 = javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(8)) }.get()

dependencies {
    implementation(files("${java8.metadata.installationPath}/lib/tools.jar"))
    annotationProcessor("dev.rdh:amnesia:1.0")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xplugin:amnesia", "-g:none"))
}

tasks.jar {
    isReproducibleFileOrder = true
    isPreserveFileTimestamps = false

    manifest {
        attributes["Implementation-Title"] = "Amnesia"
        attributes["Implementation-Version"] = version
    }
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