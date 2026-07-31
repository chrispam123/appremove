plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":core-image"))
    implementation(project(":core-ml"))
    implementation(compose.desktop.currentOs)
    testImplementation(kotlin("test"))
}

compose.desktop {
    application {
        mainClass = "com.appremove.app.MainKt"

        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi)
            packageName = "appremove"
            packageVersion = "1.0.0"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
