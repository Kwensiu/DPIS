plugins {
    alias(libs.plugins.agp.app)
}

android {
    namespace = "com.dpis.displaytool"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.kwensiu.dpis.displaytool"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.recyclerview)
    testImplementation(libs.junit4)
}
