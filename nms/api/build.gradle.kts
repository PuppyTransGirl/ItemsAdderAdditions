plugins {
    id("java")
}

val minecraftVersion: String by rootProject
val minMinecraftVersion: String by rootProject
val itemsAdderApiVersion: String by rootProject

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.devs.beer/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${minMinecraftVersion}-R0.1-SNAPSHOT")
    compileOnly("dev.lone:api-itemsadder:${itemsAdderApiVersion}")
    compileOnly("org.jspecify:jspecify:1.0.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}
