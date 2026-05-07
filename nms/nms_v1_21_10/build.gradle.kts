plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
}

val nmsVersion: String = rootProject.properties["nmsVersion_v1_21_10"] as String
val itemsAdderApiVersion: String by rootProject

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.devs.beer/")
}

dependencies {
    paperweight.paperDevBundle("${nmsVersion}-R0.1-SNAPSHOT")
    compileOnly(files("../../libs/ItemsAdder_4.0.17-beta-10-test-7.jar"))

    compileOnly(project(":nms:api"))
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}
