plugins {
    id("iaadditions.java-conventions")
    `java-library`
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.commons.io)
    compileOnly(libs.asm)
    compileOnly(libs.asm.commons)
    compileOnly(libs.itemsadder)
    compileOnly(libs.bytebuddy.agent)
    compileOnly(libs.jspecify)

    api(project(":IAAdditions-NMS:api"))
    api(project(":IAAdditions-Hook"))

    implementation(libs.bstats.bukkit)
    implementation(libs.custom.block.data)
    implementation(libs.more.persistent.data.types)
    implementation(libs.antigrieflib)
}
