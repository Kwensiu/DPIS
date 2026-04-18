plugins {
    alias(libs.plugins.agp.app)
}

android {
    namespace = "com.dpis.module"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "com.dpis.module"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            signingConfig = signingConfigs["debug"]
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
    }

    testOptions {
        unitTests.all {
            it.useJUnit()
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
    compileOnly(libs.legacy.xposed.api)
    implementation(libs.libxposed.service)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.material)
    testImplementation(libs.junit4)
}

