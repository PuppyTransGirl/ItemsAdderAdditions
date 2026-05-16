plugins {
    id("java")
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
    id("maven-publish")
}

val minecraftVersion: String by project
val minMinecraftVersion: String by project

fun enabled(name: String) = (findProperty(name) as? String)?.toBoolean() ?: true

group = "toutouchien.itemsadderadditions"
version = "1.0.8-beta-12"

repositories {
    mavenCentral()
    mavenLocal()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://maven.devs.beer/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://maven.playpro.com/")
    maven("https://repo.momirealms.net/releases/")
}

dependencies {
    compileOnly(libs.paper.api)

    // Dependencies inside Paper NMS
    compileOnly(libs.commons.io)
    compileOnly(libs.asm)
    compileOnly(libs.asm.commons)

    // NMS modules
    implementation(project(":nms:api"))
    if (enabled("enable_nms_v26_1_2")) implementation(project(":nms:nms_v26_1_2"))
    if (enabled("enable_nms_v1_21_11")) implementation(project(":nms:nms_v1_21_11"))
    if (enabled("enable_nms_v1_21_10")) implementation(project(":nms:nms_v1_21_10"))
    if (enabled("enable_nms_v1_21_8")) implementation(project(":nms:nms_v1_21_8"))
    if (enabled("enable_nms_v1_21_7")) implementation(project(":nms:nms_v1_21_7"))
    if (enabled("enable_nms_v1_21_6")) implementation(project(":nms:nms_v1_21_6"))
    if (enabled("enable_nms_v1_21_5")) implementation(project(":nms:nms_v1_21_5"))
    if (enabled("enable_nms_v1_21_4")) implementation(project(":nms:nms_v1_21_4"))
    if (enabled("enable_nms_v1_21_3")) implementation(project(":nms:nms_v1_21_3"))
    if (enabled("enable_nms_v1_21_1")) implementation(project(":nms:nms_v1_21_1"))
    if (enabled("enable_nms_v1_20_6")) implementation(project(":nms:nms_v1_20_6"))

    // Plugins
    compileOnly(libs.itemsadder)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.mythicmobs)
    compileOnly(libs.coreprotect)

    // Other
    compileOnly(libs.bytebuddy.agent)

    // Dependencies
    implementation(libs.bstats.bukkit)
    implementation(libs.custom.block.data)
    implementation(libs.more.persistent.data.types)
    implementation(libs.antigrieflib)
}

tasks {
    runServer {
        minecraftVersion(minecraftVersion)
        systemProperty(
            "net.kyori.adventure.text.warnWhenLegacyFormattingDetected",
            findProperty("net.kyori.adventure.text.warnWhenLegacyFormattingDetected") ?: false
        )
        systemProperty(
            "com.mojang.eula.agree",
            findProperty("com.mojang.eula.agree") ?: true
        )

        jvmArgs(
            "-Xmx4096M",
            "-Xms4096M",
//            "-XX:+AllowEnhancedClassRedefinition",
//            "-XX:HotswapAgent=core",
        )

        downloadPlugins {
            modrinth("PlaceholderAPI", libs.versions.placeholderapi.get())
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

        relocate(
            "org.bstats",
            "${project.group}.libs.org.bstats"
        )
        relocate(
            "com.jeff_media.customblockdata",
            "${project.group}.libs.com.jeff_media.customblockdata"
        )
        relocate(
            "com.jeff_media.morepersistentdatatypes",
            "${project.group}.libs.com.jeff_media.morepersistentdatatypes"
        )
        relocate(
            "net.momirealms.antigrieflib",
            "${project.group}.libs.net.momirealms.antigrieflib"
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
    options.isIncremental = true
    options.isFork = true
}
