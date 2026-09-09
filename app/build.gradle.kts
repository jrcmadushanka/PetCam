plugins {
    id("petcam.android.application")
    id("petcam.android.compose")
    id("petcam.hilt")
}

android {
    namespace = "com.civdevops.petcam"

    defaultConfig {
        applicationId = "com.civdevops.petcam"
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":data:settings"))
    implementation(project(":feature:settings"))
    implementation(project(":core:model"))
    implementation(project(":data:camera"))
    implementation(project(":data:media"))
    implementation(project(":feature:camera"))
    implementation(project(":domain"))

    implementation(libs.androidx.camera.viewfinder.compose)
    implementation(libs.androidx.camera.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.core.splashscreen)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}