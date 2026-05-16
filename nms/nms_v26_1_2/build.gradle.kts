plugins {
    alias(libs.plugins.paperweight.userdev)
}

val nmsVersion: String = rootProject.properties["nmsVersion_v26_1_2"] as String

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.devs.beer/")
}

dependencies {
    paperweight.paperDevBundle(nmsVersion)
    compileOnly(libs.itemsadder)
    compileOnly(project(":nms:api"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

configurations.configureEach {
    if (isCanBeResolved) {
        attributes {
            attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
        }
    }
}
