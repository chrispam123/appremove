import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    kotlin("jvm")
    kotlin("plugin.compose")
    id("org.jetbrains.compose")
}

kotlin {
    jvmToolchain(17)
}

// Compartido entre `nativeDistributions` (instalador simple, sin marca) y la
// tarea `packageMsiBranded` (instalador con el wizard "Bauhaus cromado"),
// para que ambos generen el mismo producto/versión/upgrade sin duplicar valores.
val appPackageName = "appremove"
val appPackageVersion = "1.0.0"
val appVendor = "amover"
val appDescription = "Remové fondos y comprimí tus imágenes con un modelo de IA local."
val appUpgradeUuid = "AEECA085-2B36-43A7-BC80-DBA670EB12BF"

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
            packageName = appPackageName
            packageVersion = appPackageVersion
            vendor = appVendor
            description = appDescription

            windows {
                iconFile.set(project.file("packaging/appremove.ico"))

                // Instalación por usuario (no pide permisos de administrador).
                perUserInstall = true
                shortcut = true
                menuGroup = "amover"

                // GUID fijo: así una versión futura actualiza la instalación
                // existente en vez de crear una segunda entrada duplicada.
                // No regenerar este valor entre versiones.
                upgradeUuid = appUpgradeUuid
            }
        }
    }
}

// Instalador .msi con wizard personalizado (marca "amover"): reutiliza el
// app-image que arma `createDistributable` (mismo runtime/libs/modelo que ya
// prueba `packageMsi`), pero invoca jpackage directamente con nuestro propio
// --resource-dir para que use app/packaging/wix/main.wxs en vez del wizard
// vacío que genera jpackage por defecto. `packageMsi` queda intacto como
// alternativa sin marca.
val jpackageExecutable =
    javaToolchains
        .launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) }
        .map { it.metadata.installationPath.file("bin/jpackage.exe").asFile.absolutePath }

tasks.register<Exec>("packageMsiBranded") {
    group = "compose desktop"
    description = "Genera el instalador .msi con el wizard personalizado (marca amover)."

    dependsOn("createDistributable", ":unzipWix")

    val appImageDir = layout.buildDirectory.dir("compose/binaries/main/app/$appPackageName")
    val wixToolsetDir = rootProject.layout.buildDirectory.dir("wix311")
    val wixSourceDir = project.file("packaging/wix")
    val wixResourceDir = layout.buildDirectory.dir("compose/wix-resources")
    val destDir = layout.buildDirectory.dir("compose/binaries/main/msi-branded")

    inputs.dir(wixSourceDir)
    outputs.dir(destDir)

    doFirst {
        // jpackage no copia banner.png/lateral.png a su directorio de config
        // (solo los .wxs/.wxi/.wxl que reconoce por nombre), así que
        // main.wxs los referencia por ruta absoluta. Esa ruta no puede
        // quedar hardcodeada en el archivo versionado (depende de dónde esté
        // clonado el repo), así que se resuelve acá y se vuelca a una copia
        // procesada del resource-dir dentro de build/.
        val processedDir = wixResourceDir.get().asFile
        processedDir.mkdirs()
        wixSourceDir.listFiles()?.forEach { source ->
            val target = processedDir.resolve(source.name)
            if (source.name == "main.wxs") {
                target.writeText(
                    source.readText().replace("@@WIX_ASSETS_DIR@@", wixSourceDir.absolutePath),
                )
            } else {
                source.copyTo(target, overwrite = true)
            }
        }

        destDir.get().asFile.mkdirs()
        executable = jpackageExecutable.get()
        environment(
            "PATH",
            "${wixToolsetDir.get().asFile.absolutePath}${File.pathSeparator}${System.getenv("PATH")}",
        )
        args(
            "--type", "msi",
            "--app-image", appImageDir.get().asFile.absolutePath,
            "--resource-dir", processedDir.absolutePath,
            "--dest", destDir.get().asFile.absolutePath,
            "--name", appPackageName,
            "--app-version", appPackageVersion,
            "--vendor", appVendor,
            "--description", appDescription,
            "--icon", project.file("packaging/appremove.ico").absolutePath,
            "--win-per-user-install",
            "--win-shortcut",
            "--win-menu",
            "--win-menu-group", "amover",
            "--win-upgrade-uuid", appUpgradeUuid,
        )
    }
}

tasks.test {
    useJUnitPlatform()
}
