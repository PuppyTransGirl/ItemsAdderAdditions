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
    compileOnly(libs.worldguard.bukkit)
    compileOnly(libs.worldedit.bukkit)
    compileOnly(libs.mmoitems)
    compileOnly(libs.mythiclib)
}
