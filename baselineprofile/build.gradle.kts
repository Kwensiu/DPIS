plugins {
    alias(libs.plugins.agp.test)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.dpis.module.baselineprofile"
    compileSdk = 37

    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":app"

    flavorDimensions += "xposedApi"
    productFlavors {
        create("modern") {
            dimension = "xposedApi"
        }
        create("legacy") {
            dimension = "xposedApi"
        }
    }
}

baselineProfile {
    // Use the connected API 33+ device for generation so the CI-friendly task
    // remains usable without requiring a rooted Gradle-managed device.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.uiautomator)
}
