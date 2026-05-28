import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("iaadditions.java-conventions")
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
    id("maven-publish")
}

val minMinecraftVersion: String by rootProject

fun enabled(name: String) = (rootProject.findProperty(name) as? String)?.toBoolean() ?: true

dependencies {
    implementation(project(":IAAdditions-Core"))

    if (enabled("enable_nms_v26_1_2")) implementation(project(":IAAdditions-NMS:IAAdditions-NMS_v26_1_2"))
    if (enabled("enable_nms_v1_21_11")) implementation(project(":IAAdditions-NMS:IAAdditions-NMS_v1_21_11"))
    if (enabled("enable_nms_v1_21_10")) implementation(project(":IAAdditions-NMS:IAAdditions-NMS_v1_21_10"))
    if (enabled("enable_nms_v1_21_8")) implementation(project(":IAAdditions-NMS:IAAdditions-NMS_v1_21_8"))
    if (enabled("enable_nms_v1_21_7")) implementation(project(":IAAdditions-NMS:IAAdditions-NMS_v1_21_7"))
    if (enabled("enable_nms_v1_21_6")) implementation(project(":IAAdditions-NMS:IAAdditions-NMS_v1_21_6"))
    if (enabled("enable_nms_v1_21_5")) implementation(project(":IAAdditions-NMS:IAAdditions-NMS_v1_21_5"))
    if (enabled("enable_nms_v1_21_4")) implementation(project(":IAAdditions-NMS:IAAdditions-NMS_v1_21_4"))
    if (enabled("enable_nms_v1_21_3")) implementation(project(":IAAdditions-NMS:IAAdditions-NMS_v1_21_3"))
    if (enabled("enable_nms_v1_21_1")) implementation(project(":IAAdditions-NMS:IAAdditions-NMS_v1_21_1"))
    if (enabled("enable_nms_v1_20_6")) implementation(project(":IAAdditions-NMS:IAAdditions-NMS_v1_20_6"))
}

tasks {
    shadowJar {
        archiveFileName.set("${rootProject.name}-${rootProject.version}.jar")

        relocate("org.bstats", "${rootProject.group}.libs.org.bstats")
        relocate("com.jeff_media.customblockdata", "${rootProject.group}.libs.com.jeff_media.customblockdata")
        relocate(
            "com.jeff_media.morepersistentdatatypes",
            "${rootProject.group}.libs.com.jeff_media.morepersistentdatatypes"
        )
        relocate("net.momirealms.antigrieflib", "${rootProject.group}.libs.net.momirealms.antigrieflib")
    }

    build {
        dependsOn("shadowJar")
    }

    processResources {
        filteringCharset = "UTF-8"

        val props = mapOf(
            "version" to rootProject.version,
            "minMinecraftVersion" to minMinecraftVersion
        )

        inputs.properties(props)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }

        from(rootProject.files("LICENSE", "THIRD_PARTY_LICENSES.md")) {
            into("META-INF")
        }
    }

    fun xyz.jpenilla.runpaper.task.RunServer.commonServerConfig(version: String) {
        runDirectory(rootProject.layout.projectDirectory.dir("run/$version").asFile)
        systemProperty(
            "net.kyori.adventure.text.warnWhenLegacyFormattingDetected",
            rootProject.findProperty("net.kyori.adventure.text.warnWhenLegacyFormattingDetected") ?: false
        )
        systemProperty("com.mojang.eula.agree", rootProject.findProperty("com.mojang.eula.agree") ?: true)
        jvmArgs("-Xmx4096M", "-Xms4096M")
        javaLauncher.set(project.javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(25))
        })
        downloadPlugins {
            modrinth("PlaceholderAPI", libs.versions.placeholderapi.get())
        }
    }

    runServer {
        minecraftVersion("1.21.11")
        commonServerConfig("1.21.11")
    }

    register<xyz.jpenilla.runpaper.task.RunServer>("runServer1_20_6") {
        group = "run paper"
        description = "Run a Paper 1.20.6 server for plugin testing."
        minecraftVersion("1.20.6")
        commonServerConfig("1.20.6")
    }

    register<xyz.jpenilla.runpaper.task.RunServer>("runServer1_21_4") {
        group = "run paper"
        description = "Run a Paper 1.21.4 server for plugin testing."
        minecraftVersion("1.21.4")
        commonServerConfig("1.21.4")
    }

    register<xyz.jpenilla.runpaper.task.RunServer>("runServer26_1_2") {
        group = "run paper"
        description = "Run a Paper 26.1.2 server for plugin testing."
        minecraftVersion("26.1.2")
        commonServerConfig("26.1.2")
    }

    val releaseDir = rootProject.layout.buildDirectory.dir("release")
    val platforms = listOf("Hangar", "SpigotMC", "Modrinth")

    val platformTasks = platforms.map { platform ->
        register<ShadowJar>("shadowJar$platform") {
            group = "build"
            description = "Builds the $platform distribution jar"

            archiveBaseName.set(rootProject.name)
            archiveVersion.set(rootProject.version.toString())
            archiveClassifier.set(platform.lowercase())
            destinationDirectory.set(releaseDir)

            from(sourceSets.main.get().output)
            configurations.set(setOf(project.configurations["runtimeClasspath"]))

            filesMatching("iaadditions_platform.properties") {
                filter { line: String ->
                    if (line.startsWith("platform=")) "platform=$platform" else line
                }
            }

            relocate("org.bstats", "${rootProject.group}.libs.org.bstats")
            relocate("com.jeff_media.customblockdata", "${rootProject.group}.libs.com.jeff_media.customblockdata")
            relocate(
                "com.jeff_media.morepersistentdatatypes",
                "${rootProject.group}.libs.com.jeff_media.morepersistentdatatypes"
            )
            relocate("net.momirealms.antigrieflib", "${rootProject.group}.libs.net.momirealms.antigrieflib")
        }
    }

    register("release") {
        group = "build"
        description = "Builds one jar per distribution platform into build/release/"
        dependsOn(platformTasks)
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    archiveClassifier.set("sources")
    from(project(":IAAdditions-Core").sourceSets["main"].allSource)
}

artifacts {
    add("archives", sourcesJar)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
            groupId = rootProject.group.toString()
            artifactId = rootProject.name
            version = rootProject.version.toString()
        }
    }
    repositories {
        mavenLocal()
    }
}
