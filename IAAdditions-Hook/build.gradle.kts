plugins {
    id("iaadditions.java-conventions")
    `java-library`
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.jspecify)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.mythicmobs)
    compileOnly(libs.coreprotect)
}
