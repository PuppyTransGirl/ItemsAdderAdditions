plugins {
    id("java")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.3.1"
    id("maven-publish")
}

val minecraftVersion: String by project
val minMinecraftVersion: String by project
val itemsAdderApiVersion: String by project
val placeholderApiVersion: String by project
val mythicMobsVersion: String by project
val coreProtectVersion: String by project
val byteBuddyAgentVersion: String by project
val bStatsVersion: String by project
val customBlockDataVersion: String by project
val morePersistentDataTypesVersion: String by project

group = "toutouchien.itemsadderadditions"
version = "1.0.5-beta-5"

repositories {
    mavenCentral()
    mavenLocal()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://maven.devs.beer/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://maven.playpro.com/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // Dependencies inside Paper NMS
    compileOnly("commons-io:commons-io:2.21.0")
    compileOnly("org.ow2.asm:asm:9.9.1")
    compileOnly("org.ow2.asm:asm-commons:9.9.1")

    // NMS modules
    implementation(project(":nms:api"))
    implementation(project(":nms:nms_v26_1_1"))
    implementation(project(":nms:nms_v1_21_11"))
    implementation(project(":nms:nms_v1_21_10"))
    implementation(project(":nms:nms_v1_21_8"))
    implementation(project(":nms:nms_v1_21_7"))
    implementation(project(":nms:nms_v1_21_6"))
    implementation(project(":nms:nms_v1_21_5"))
    implementation(project(":nms:nms_v1_21_4"))
    implementation(project(":nms:nms_v1_21_3"))
    implementation(project(":nms:nms_v1_21_1"))
    implementation(project(":nms:nms_v1_20_6"))

    // Plugins
    compileOnly("dev.lone:api-itemsadder:${itemsAdderApiVersion}")
    compileOnly("me.clip:placeholderapi:${placeholderApiVersion}")
    compileOnly("io.lumine:Mythic-Dist:${mythicMobsVersion}")
    compileOnly("net.coreprotect:coreprotect:${coreProtectVersion}")

    // Other
    compileOnly("net.bytebuddy:byte-buddy-agent:${byteBuddyAgentVersion}")

    // Dependencies
    implementation("com.jeff-media:custom-block-data:${customBlockDataVersion}")
    implementation("com.jeff-media:MorePersistentDataTypes:${morePersistentDataTypesVersion}")
    implementation("org.bstats:bstats-bukkit:${bStatsVersion}")
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
//            modrinth("LuckPerms", "v5.5.17-bukkit")
//            modrinth("TabTPS", "1.3.30")
//            modrinth("ServerLogViewer-Paper", "1.0.0")
//            modrinth("PlaceholderAPI", placeholderApiVersion)
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

        // Original relocation
        relocate("org.bstats", "${project.group}.libs.org.bstats")

        relocate("com.jeff_media.customblockdata", "${project.group}.libs.com.jeff_media.customblockdata")
        relocate(
            "com.jeff_media.morepersistentdatatypes",
            "${project.group}.libs.com.jeff_media.morepersistentdatatypes"
        )
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

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}
