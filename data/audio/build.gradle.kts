plugins {
    id("petcam.android.library")
    id("petcam.hilt")
}

android {
    namespace = "com.civdevops.petcam.data.audio"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":domain"))
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.junit)
}