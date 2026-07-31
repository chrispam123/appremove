plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":domain"))
    // Build de CPU (sin CUDA): es una app de escritorio general, no se puede
    // asumir que haya una GPU disponible.
    implementation("com.microsoft.onnxruntime:onnxruntime:1.28.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
