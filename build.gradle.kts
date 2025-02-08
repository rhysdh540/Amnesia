plugins {
    id("java")
}

group = "dev.rdh"
version = "0.0-BOOTSTRAP"

base.archivesName = "amnesia"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

val java8 = javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(8)) }.get()

dependencies {
    implementation(files("${java8.metadata.installationPath}/lib/tools.jar"))
}
