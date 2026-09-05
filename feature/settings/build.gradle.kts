plugins {
    id("petcam.android.library")
    id("petcam.android.compose")
    id("petcam.hilt")
}

android {
    namespace = "com.civdevops.petcam.feature.settings"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:designsystem"))
    implementation(project(":domain"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    implementation(
        libs.androidx.hilt.lifecycle.viewmodel.compose,
    )

    implementation(
        libs.androidx.compose.material3.adaptive,
    )
}