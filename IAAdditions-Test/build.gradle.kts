plugins {
    id("iaadditions.java-conventions")
}

// MockBukkit requires Java 25+ - override the convention plugin default of 21.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.test {
    useJUnitPlatform()
    outputs.upToDateWhen { false }
}

tasks.compileTestJava {
    options.release.set(25)
}

// IAAdditions-Test has no main sources of its own. Point the coverage report at
// IAAdditions-Core so it measures the production classes exercised by these tests.
tasks.jacocoTestReport {
    val core = project(":IAAdditions-Core")
    val coreSourceSets = core.extensions.getByType<SourceSetContainer>()
    val coreMain = coreSourceSets.named("main").get()
    additionalClassDirs(coreMain.output.classesDirs)
    additionalSourceDirs(files(coreMain.allSource.srcDirs))
    sourceDirectories.from(files(coreMain.allSource.srcDirs))
    classDirectories.from(coreMain.output.classesDirs)

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

dependencies {
    testImplementation(project(":IAAdditions-Core"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.paper.api.test)
    testImplementation(libs.mockbukkit)
    testImplementation(libs.itemsadder)
    testImplementation(libs.mockito.core)
    testImplementation(libs.asm)
    testImplementation(libs.asm.commons)
}
