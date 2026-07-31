plugins {
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.compose") version "2.0.21" apply false
    id("org.jetbrains.compose") version "1.7.0" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}

allprojects {
    repositories {
        mavenCentral()
        google()
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}
