plugins {
    id("petcam.android.library")
    id("petcam.hilt")
}

android {
    namespace = "com.civdevops.petcam.data.settings"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":domain"))

    implementation(libs.androidx.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
}