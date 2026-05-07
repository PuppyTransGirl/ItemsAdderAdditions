plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

val nmsVersion: String = rootProject.properties["nmsVersion_v26_1_2"] as String
val itemsAdderApiVersion: String by rootProject

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.devs.beer/")
}

dependencies {
    paperweight.paperDevBundle(nmsVersion)
    compileOnly(files("../../libs/ItemsAdder_4.0.17-beta-10-test-7.jar"))
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
