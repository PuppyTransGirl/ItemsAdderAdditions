group = "toutouchien.itemsadderadditions"
version = "1.0.11"

tasks.register("fullTest") {
    group = "verification"
    description = "Boots every enabled Paper version with ItemsAdderAdditions installed."
    dependsOn(":IAAdditions-Jar:fullTest")
}
