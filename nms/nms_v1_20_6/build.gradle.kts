plugins {
    alias(libs.plugins.paperweight.userdev)
}

val nmsVersion: String = rootProject.properties["nmsVersion_v1_20_6"] as String

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.devs.beer/")
}

dependencies {
    paperweight.paperDevBundle("${nmsVersion}-R0.1-SNAPSHOT")
    compileOnly(libs.itemsadder)
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
