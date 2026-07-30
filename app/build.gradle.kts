plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":core-image"))
    implementation(project(":core-ml"))
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.appremove.app.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
