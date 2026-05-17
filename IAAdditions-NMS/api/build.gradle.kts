plugins {
    id("iaadditions.java-conventions")
}

val minMinecraftVersion: String by rootProject

dependencies {
    compileOnly("io.papermc.paper:paper-api:${minMinecraftVersion}-R0.1-SNAPSHOT")
    compileOnly(libs.itemsadder)
    compileOnly(libs.jspecify)
}
