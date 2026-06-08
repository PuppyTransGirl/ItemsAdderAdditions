import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

plugins {
    id("iaadditions.java-conventions")
    alias(libs.plugins.run.paper)
    alias(libs.plugins.shadow)
    id("maven-publish")
}

val minMinecraftVersion: String by rootProject

fun enabled(name: String) = (rootProject.findProperty(name) as? String)?.toBoolean() ?: true

// Single source for fullTest versions: enabled nmsVersion_* entries from gradle.properties.
// Add a new nmsVersion_vX_Y_Z plus enable_nms_vX_Y_Z there and fullTest picks it up.
data class FullTestVersion(val id: String, val minecraftVersion: String)

fun fullTestMinecraftVersion(value: String): String = value.removeSuffix(".build.+")

fun minecraftVersionParts(value: String): List<Int> = value.split('.').map { part -> part.toIntOrNull() ?: 0 }

fun compareMinecraftVersions(left: String, right: String): Int {
    val leftParts = minecraftVersionParts(left)
    val rightParts = minecraftVersionParts(right)
    val size = maxOf(leftParts.size, rightParts.size)
    for (index in 0 until size) {
        val comparison = (leftParts.getOrNull(index) ?: 0).compareTo(rightParts.getOrNull(index) ?: 0)
        if (comparison != 0) return comparison
    }
    return 0
}

fun serverJavaVersion(@Suppress("UNUSED_PARAMETER") minecraftVersion: String): Int = 25

fun classFileMajorToJavaVersion(major: Int): Int = major - 44

val fullTestVersions = rootProject.properties.entries.asSequence()
    .map { it.key to it.value.toString() }
    .filter { (key, _) -> key.startsWith("nmsVersion_") }
    .map { (key, value) ->
        val id = key.removePrefix("nmsVersion_")
        FullTestVersion(id, fullTestMinecraftVersion(value))
    }
    .filter { enabled("enable_nms_${it.id}") }
    .sortedWith { left, right -> compareMinecraftVersions(left.minecraftVersion, right.minecraftVersion) }
    .toList()

fun requiredFullTestPlugin(displayName: String, vararg fileNameTokens: String) = rootProject.layout.file(
    rootProject.providers.provider {
        val pluginsDir = rootProject.layout.projectDirectory.dir("test-servers/test-plugins").asFile
        val matches = pluginsDir.listFiles { file ->
            file.isFile &&
                file.extension.equals("jar", true) &&
                fileNameTokens.all { token -> file.name.contains(token, true) }
        }?.sortedBy { it.name.lowercase() }.orEmpty()
        if (matches.isEmpty()) {
            throw GradleException(
                "Missing required $displayName jar for fullTest. Drop it in ${pluginsDir.path}/ " +
                    "with filename containing: ${fileNameTokens.joinToString(", ")}."
            )
        }
        matches.first()
    }
)

fun fullTestProtocolLibFlavor(minecraftVersion: String): String =
    if (compareMinecraftVersions(minecraftVersion, "1.21.8") <= 0) "legacy" else "modern"

fun requiredFullTestProtocolLib(minecraftVersion: String) = requiredFullTestPlugin(
    "ProtocolLib ${fullTestProtocolLibFlavor(minecraftVersion)}",
    "protocollib",
    fullTestProtocolLibFlavor(minecraftVersion)
)

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

    fun xyz.jpenilla.runpaper.task.RunServer.commonServerConfigFor(version: String, baseDirectory: String) {
        runDirectory(rootProject.layout.projectDirectory.dir("$baseDirectory/$version").asFile)
        systemProperty(
            "net.kyori.adventure.text.warnWhenLegacyFormattingDetected",
            rootProject.findProperty("net.kyori.adventure.text.warnWhenLegacyFormattingDetected") ?: false
        )
        systemProperty("com.mojang.eula.agree", rootProject.findProperty("com.mojang.eula.agree") ?: true)
        systemProperty("Paper.IgnoreJavaVersion", true)
        jvmArgs("-Xmx4096M", "-Xms4096M")
        javaLauncher.set(project.javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(serverJavaVersion(version)))
        })
        downloadPlugins {
            modrinth("PlaceholderAPI", libs.versions.placeholderapi.get())
        }
    }

    fun xyz.jpenilla.runpaper.task.RunServer.commonServerConfig(version: String) {
        commonServerConfigFor(version, "run")
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

    val fullTestRunTasks = fullTestVersions.map { fullTestVersion ->
        register<xyz.jpenilla.runpaper.task.RunServer>("fullTestRunServer_${fullTestVersion.id}") {
            group = "verification"
            description = "Run a Paper ${fullTestVersion.minecraftVersion} server for fullTest."
            minecraftVersion(fullTestVersion.minecraftVersion)
            commonServerConfigFor(fullTestVersion.minecraftVersion, "test-servers")
            pluginJars(
                shadowJar.flatMap { it.archiveFile },
                requiredFullTestPlugin("ItemsAdder", "itemsadder"),
                requiredFullTestProtocolLib(fullTestVersion.minecraftVersion)
            )
            dependsOn(shadowJar)
        }
    }

    register<Delete>("cleanFullTestServers") {
        group = "verification"
        description = "Deletes per-version fullTest server folders under test-servers/."
        delete(fullTestVersions.map { rootProject.layout.projectDirectory.dir("test-servers/${it.minecraftVersion}") })
    }

    register("fullTest") {
        group = "verification"
        description = "Boots every enabled Paper version with ItemsAdderAdditions installed."
        dependsOn(shadowJar)

        doLast {
            val pluginsDir = rootProject.layout.projectDirectory.dir("test-servers/test-plugins").asFile
            val iaJar = requiredFullTestPlugin("ItemsAdder", "itemsadder").get().asFile
            logger.lifecycle("Using fullTest ItemsAdder jar: ${iaJar.name}")
            logger.lifecycle("Using ProtocolLib legacy for Paper <= 1.21.8, modern otherwise.")

            fun md5(file: File): String {
                val digest = MessageDigest.getInstance("MD5")
                file.inputStream().use { stream ->
                    val buffer = ByteArray(8192)
                    var read = stream.read(buffer)
                    while (read >= 0) {
                        digest.update(buffer, 0, read)
                        read = stream.read(buffer)
                    }
                }
                return digest.digest().joinToString("") { "%02x".format(it) }
            }

            val jarHash = md5(shadowJar.get().archiveFile.get().asFile)
            val cacheFile = rootProject.layout.projectDirectory.file("test-servers/.fulltest-cache.properties").asFile
            val passCache = Properties().apply {
                if (cacheFile.isFile) cacheFile.inputStream().use { load(it) }
            }
            fun savePassCache() {
                cacheFile.parentFile.mkdirs()
                cacheFile.outputStream().use { passCache.store(it, "fullTest pass cache: minecraftVersion -> passing shadowJar md5") }
            }

            val isWindows = System.getProperty("os.name").contains("Windows", ignoreCase = true)
            val gradlew = rootProject.layout.projectDirectory.file(if (isWindows) "gradlew.bat" else "gradlew").asFile
            val results = mutableListOf<Pair<String, String>>()

            fun destroyProcessTree(process: Process, forcibly: Boolean) {
                val handles = (process.toHandle().descendants().toList().asReversed() + process.toHandle())
                handles.forEach { handle ->
                    if (handle.isAlive) {
                        if (forcibly) handle.destroyForcibly() else handle.destroy()
                    }
                }
            }

            fun stopServer(process: Process, graceful: Boolean): String? {
                if (!process.isAlive) {
                    val exit = process.exitValue()
                    return if (exit == 0) null else "server process exited with code $exit"
                }

                if (graceful) {
                    runCatching {
                        OutputStreamWriter(process.outputStream, StandardCharsets.UTF_8).apply {
                            write("stop")
                            write(System.lineSeparator())
                            flush()
                        }
                    }
                    if (process.waitFor(45, TimeUnit.SECONDS)) {
                        val exit = process.exitValue()
                        return if (exit == 0) null else "server process exited with code $exit"
                    }
                }

                destroyProcessTree(process, forcibly = false)
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    destroyProcessTree(process, forcibly = true)
                    process.waitFor(10, TimeUnit.SECONDS)
                    return if (graceful) {
                        "server did not stop cleanly and was force killed"
                    } else {
                        "server was force killed after startup failure"
                    }
                }
                return if (graceful) "server did not stop cleanly and was killed" else null
            }

            fun deleteSessionLocks(fullTestVersion: FullTestVersion) {
                val serverDir = rootProject.layout.projectDirectory.dir("test-servers/${fullTestVersion.minecraftVersion}").asFile
                if (!serverDir.isDirectory) return

                val deleted = serverDir.walkTopDown()
                    .filter { file -> file.isFile && file.name == "session.lock" }
                    .count { file -> file.delete() }
                if (deleted > 0) {
                    logger.lifecycle("Deleted $deleted session.lock file(s) for Paper ${fullTestVersion.minecraftVersion}.")
                }
            }

            fun runVersion(fullTestVersion: FullTestVersion): String? {
                val protocolLibJar = requiredFullTestProtocolLib(fullTestVersion.minecraftVersion).get().asFile
                logger.lifecycle("Using ProtocolLib jar for Paper ${fullTestVersion.minecraftVersion}: ${protocolLibJar.name}")
                deleteSessionLocks(fullTestVersion)

                val taskPath = ":IAAdditions-Jar:fullTestRunServer_${fullTestVersion.id}"
                val process = ProcessBuilder(
                    gradlew.absolutePath,
                    "--console=plain",
                    "--no-daemon",
                    taskPath
                )
                    .directory(rootProject.projectDir)
                    .redirectErrorStream(true)
                val startedProcess = process.start()

                val output = LinkedBlockingQueue<String>()
                val reader = Thread {
                    startedProcess.inputStream.bufferedReader().useLines { lines ->
                        lines.forEach { line ->
                            logger.lifecycle("[${fullTestVersion.minecraftVersion}] $line")
                            output.offer(line)
                        }
                    }
                }
                reader.isDaemon = true
                reader.start()

                val deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(7)
                var sawDone = false
                var sawIaaReloadComplete = false
                var failure: String? = null
                val donePattern = Regex("Done \\([0-9.]+s\\)! For help, type \\\"help\\\"")
                val iaaReloadPattern = Regex("IAAdditions\\) \\[Reload] Reload complete\\.")
                val unsupportedClassMajorPattern = Regex("Unsupported class file major version (\\d+)", RegexOption.IGNORE_CASE)
                val ansiControlPattern = Regex("\\u001B\\[[0-?]*[ -/]*[@-~]")
                val benignPatterns = listOf(
                    Regex("A Java agent has been loaded dynamically", RegexOption.IGNORE_CASE)
                )
                val failurePatterns = listOf(
                    Regex("Fatal error trying to convert", RegexOption.IGNORE_CASE),
                    Regex("Error occurred while (loading|enabling|disabling) (ItemsAdderAdditions|ItemsAdder|ProtocolLib)", RegexOption.IGNORE_CASE),
                    Regex("Could not (load|enable) .*(ItemsAdderAdditions|ItemsAdder|ProtocolLib)", RegexOption.IGNORE_CASE),
                    Regex("\\b(ERROR|SEVERE)\\b.*(ItemsAdderAdditions|IAAdditions|ItemsAdder|ProtocolLib)", RegexOption.IGNORE_CASE),
                    Regex("Encountered an unexpected exception", RegexOption.IGNORE_CASE),
                    Regex("java\\.lang\\.[A-Za-z0-9_]*(Exception|Error)", RegexOption.IGNORE_CASE),
                    Regex("\\bat .*ItemsAdderAdditions.*toutouchien\\.itemsadderadditions")
                )

                fun processLine(line: String) {
                    val normalizedLine = ansiControlPattern.replace(line, "")
                    if (donePattern.containsMatchIn(normalizedLine)) sawDone = true
                    if (iaaReloadPattern.containsMatchIn(normalizedLine)) sawIaaReloadComplete = true

                    val unsupportedClassMajor = unsupportedClassMajorPattern.find(normalizedLine)
                    if (unsupportedClassMajor != null) {
                        val major = unsupportedClassMajor.groupValues[1].toIntOrNull()
                        failure = if (major != null) {
                            "unsupported Java ${classFileMajorToJavaVersion(major)} class file (major $major): $normalizedLine"
                        } else {
                            "unsupported class file version: $normalizedLine"
                        }
                        return
                    }

                    if (benignPatterns.any { it.containsMatchIn(normalizedLine) }) return

                    val matchedFailure = failurePatterns.firstOrNull { it.containsMatchIn(normalizedLine) }
                    if (matchedFailure != null) failure = "failure log matched: $normalizedLine"
                }

                while (failure == null && (!sawDone || !sawIaaReloadComplete)) {
                    var line = output.poll(1, TimeUnit.SECONDS)
                    while (line != null && failure == null) {
                        processLine(line)
                        line = output.poll()
                    }
                    if (failure != null || (sawDone && sawIaaReloadComplete)) break
                    if (!startedProcess.isAlive) {
                        val exit = startedProcess.waitFor()
                        failure = "server process exited before success signal with code $exit"
                        break
                    }
                    if (System.nanoTime() > deadline) {
                        failure = "startup timeout waiting for Done and ItemsAdderAdditions reload complete signals"
                    }
                }

                if (failure == null) {
                    val settleDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
                    while (failure == null && startedProcess.isAlive && System.nanoTime() < settleDeadline) {
                        var line = output.poll(1, TimeUnit.SECONDS)
                        while (line != null && failure == null) {
                            processLine(line)
                            line = output.poll()
                        }
                    }
                }

                val stopFailure = stopServer(startedProcess, graceful = failure == null)
                reader.join(5000)
                return failure ?: stopFailure
            }

            for (fullTestVersion in fullTestVersions) {
                val version = fullTestVersion.minecraftVersion
                if (passCache.getProperty(version) == jarHash) {
                    results += version to "PASS (skipped, jar unchanged since last pass)"
                    logger.lifecycle("fullTest $version: PASS (skipped, jar unchanged since last pass)")
                    continue
                }
                logger.lifecycle("Starting fullTest for Paper $version in test-servers/$version/")
                val failure = runVersion(fullTestVersion)
                if (failure == null) {
                    passCache.setProperty(version, jarHash)
                    savePassCache()
                    results += version to "PASS"
                    logger.lifecycle("fullTest $version: PASS")
                } else {
                    passCache.remove(version)
                    savePassCache()
                    results += version to "FAIL: $failure"
                    logger.lifecycle("fullTest $version: FAIL: $failure")
                    break
                }
            }

            logger.lifecycle("fullTest results:")
            results.forEach { (version, result) -> logger.lifecycle("  $version: $result") }
            val failures = results.filter { it.second.startsWith("FAIL") }
            if (failures.isNotEmpty()) {
                throw GradleException("fullTest failed for ${failures.size} version(s). Required jars are read from ${pluginsDir.path}/.")
            }
        }
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
