plugins {
    id("iaadditions.java-conventions")
}

// MockBukkit requires Java 25+ — override the convention plugin default of 21.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileTestJava {
    options.release.set(25)
}

dependencies {
    testImplementation(project(":IAAdditions-Core"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.paper.api.test)
    testImplementation(libs.mockbukkit)
}
