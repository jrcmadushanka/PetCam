plugins {
    id("petcam.android.library")
    id("petcam.hilt")
}

android {
    namespace = "com.civdevops.petcam.data.camera"
}

dependencies {
    implementation(project(":core:model"))

    api(libs.androidx.camera.core)

    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)

    implementation(libs.kotlinx.coroutines.core)
}