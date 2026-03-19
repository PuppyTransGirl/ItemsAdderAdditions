plugins {
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.3.1"
    id("maven-publish")
}

val minecraftVersion: String by project
val minMinecraftVersion: String by project
val itemsAdderApiVersion: String by project
val placeholderApiVersion: String by project
val bStatsVersion: String by project

group = "toutouchien.itemsadderadditions"
version = "1.0.0"

repositories {
    mavenCentral()
    mavenLocal()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://maven.devs.beer/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    paperweight.paperDevBundle("${minecraftVersion}-R0.1-SNAPSHOT")

    // Plugins
    compileOnly("dev.lone:api-itemsadder:${itemsAdderApiVersion}")
    compileOnly("me.clip:placeholderapi:${placeholderApiVersion}")

    // Dependencies
    implementation("org.bstats:bstats-bukkit:${bStatsVersion}")
}

paperweight {
    addServerDependencyTo = configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).map { setOf(it) }
}

tasks {
    runServer {
        minecraftVersion(minecraftVersion)

        jvmArgs(
            "-Xmx4096M",
            "-Xms4096M",
            "-XX:+AllowEnhancedClassRedefinition",
            "-XX:HotswapAgent=core",
            "-Dcom.mojang.eula.agree=true"
        )

        downloadPlugins {
            modrinth("LuckPerms", "v5.5.17-bukkit")
            github("jpenilla", "TabTPS", "v1.3.29", "tabtps-paper-1.3.29.jar")
            modrinth("ServerLogViewer-Paper", "1.0.0")
            modrinth("PlaceholderAPI", placeholderApiVersion)
        }
    }

    build {
        dependsOn("jar")
    }

    javadoc {
        isFailOnError = false
        options.encoding = "UTF-8"
    }

    register<Jar>("javadocJar") {
        dependsOn(javadoc)
        archiveClassifier.set("javadoc")
        from(javadoc.get().destinationDir)
    }

    register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }

    shadowJar {
        archiveFileName.set("${project.name}-${project.version}.jar")

        relocate("org.bstats", "${project.group}.libs.org.bstats")
    }

    processResources {
        filteringCharset = "UTF-8"

        val props = mapOf(
            "version" to version,
            "minMinecraftVersion" to minMinecraftVersion
        )

        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }

        from(listOf("LICENSE", "THIRD_PARTY_LICENSES.md")) {
            into("META-INF")
        }
    }
}

artifacts {
    add("archives", tasks.named("sourcesJar"))
    add("archives", tasks.named("javadocJar"))
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(tasks.named("sourcesJar"))
            artifact(tasks.named("javadocJar"))
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
        }
    }
    repositories {
        mavenLocal()
    }
}
