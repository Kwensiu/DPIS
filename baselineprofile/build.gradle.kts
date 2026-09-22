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
        testInstrumentationRunnerArguments["androidx.benchmark.suppressErrors"] = "EMULATOR"
    }

    testOptions {
        animationsDisabled = true
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    implementation(libs.androidx.test.core)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.uiautomator)
}

androidComponents {
    onVariants { variant ->
        val artifactsLoader = variant.artifacts.getBuiltArtifactsLoader()
        variant.instrumentationRunnerArguments.put(
            "targetAppId",
            variant.testedApks.map { artifactsLoader.load(it)?.applicationId },
        )
    }
}
