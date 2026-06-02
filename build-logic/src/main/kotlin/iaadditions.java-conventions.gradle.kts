plugins {
    java
    jacoco
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/") // Paper
    maven("https://jitpack.io")
    maven("https://maven.devs.beer/") // ItemsAdder pre 4.0.17
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PlaceholderAPI
    maven("https://mvn.lumine.io/repository/maven-public/") // MythicMobs
    maven("https://maven.playpro.com/")
    maven("https://repo.momirealms.net/releases/") // AntiGriefLib
    maven("https://nexus.phoenixdevt.fr/repository/maven-public/") // MMOItems
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.isIncremental = true
    options.isFork = true
}

tasks.withType<Javadoc>().configureEach {
    isFailOnError = false
    options.encoding = "UTF-8"
}

jacoco {
    toolVersion = "0.8.14"
}

tasks.test {
    finalizedBy("jacocoTestReport")
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
}
