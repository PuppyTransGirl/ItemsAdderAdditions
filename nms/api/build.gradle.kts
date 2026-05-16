plugins {
    id("java")
}

val minMinecraftVersion: String by rootProject

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.devs.beer/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${minMinecraftVersion}-R0.1-SNAPSHOT")
    compileOnly(libs.itemsadder)
    compileOnly(libs.jspecify)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
