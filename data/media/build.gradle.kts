plugins {
    id("petcam.android.library")
    id("petcam.hilt")
}

android {
    namespace = "com.civdevops.petcam.data.media"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":data:camera"))
    implementation(libs.kotlinx.coroutines.core)
}