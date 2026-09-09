plugins {
    id("petcam.android.library")
    id("petcam.hilt")
}

android {
    namespace = "com.civdevops.petcam.data.camera"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":domain"))
    implementation(project(":data:media"))
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.video)
    implementation(libs.kotlinx.coroutines.core)
    api(libs.androidx.camera.core)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}