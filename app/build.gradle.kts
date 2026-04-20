plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.aboutlibraries)
}

private val appVersionName = "1.1.0" // x-release-please-version

private fun semVerToVersionCode(version: String): Int {
    val parts = version.split(".")
    require(parts.size == 3) {
        "Version must use semantic versioning (major.minor.patch): $version"
    }

    val major = parts[0].toIntOrNull()
    val minor = parts[1].toIntOrNull()
    val patch = parts[2].toIntOrNull()
    require(major != null && minor != null && patch != null) {
        "Version segments must be numeric: $version"
    }
    require(major in 0..99 && minor in 0..99 && patch in 0..99) {
        "Version segments must stay in 0..99 for versionCode mapping: $version"
    }

    return major * 10_000 + minor * 100 + patch
}

android {
    namespace = "com.dpis.module"
    compileSdk = 36
    buildToolsVersion = "36.1.0"

    defaultConfig {
        applicationId = "io.github.kwensiu.dpis"
        minSdk = 26
        targetSdk = 36
        versionName = appVersionName
        versionCode = semVerToVersionCode(appVersionName)
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

