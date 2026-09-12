plugins {
    id("petcam.android.library")
    id("petcam.android.compose")
}

android {
    namespace = "com.civdevops.petcam.core.designsystem"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.animation.core)

    testImplementation(kotlin("test"))
}