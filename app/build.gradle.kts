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
            vendor = "amover"
            description = "Remové fondos y comprimí tus imágenes con un modelo de IA local."

            windows {
                iconFile.set(project.file("packaging/appremove.ico"))

                // Instalación por usuario (no pide permisos de administrador).
                perUserInstall = true
                shortcut = true
                menuGroup = "amover"

                // GUID fijo: así una versión futura actualiza la instalación
                // existente en vez de crear una segunda entrada duplicada.
                // No regenerar este valor entre versiones.
                upgradeUuid = "AEECA085-2B36-43A7-BC80-DBA670EB12BF"
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
