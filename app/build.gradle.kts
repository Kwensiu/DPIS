plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.aboutlibraries)
}

private val appVersionName = "1.3.0" // x-release-please-version

private fun readGradleOrEnv(name: String): String? {
    val gradleValue = project.findProperty(name)?.toString()?.trim()
    if (!gradleValue.isNullOrEmpty()) {
        return gradleValue
    }
    val envValue = System.getenv(name)?.trim()
    return if (envValue.isNullOrEmpty()) null else envValue
}

private val releaseStoreFilePath = readGradleOrEnv("DPIS_RELEASE_STORE_FILE")
private val releaseStorePassword = readGradleOrEnv("DPIS_RELEASE_STORE_PASSWORD")
private val releaseKeyAlias = readGradleOrEnv("DPIS_RELEASE_KEY_ALIAS")
private val releaseKeyPassword = readGradleOrEnv("DPIS_RELEASE_KEY_PASSWORD")

private val releaseSigningMissingKeys = buildList {
    if (releaseStoreFilePath.isNullOrEmpty()) add("DPIS_RELEASE_STORE_FILE")
    if (releaseStorePassword.isNullOrEmpty()) add("DPIS_RELEASE_STORE_PASSWORD")
    if (releaseKeyAlias.isNullOrEmpty()) add("DPIS_RELEASE_KEY_ALIAS")
    if (releaseKeyPassword.isNullOrEmpty()) add("DPIS_RELEASE_KEY_PASSWORD")
}

private val hasReleaseSigningConfig = releaseSigningMissingKeys.isEmpty()
private val releaseTasksRequested = gradle.startParameter.taskNames.any {
    it.contains("Release", ignoreCase = true)
}

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

    signingConfigs {
        create("release") {
            if (hasReleaseSigningConfig) {
                storeFile = file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles("proguard-rules.pro")
            signingConfig = if (hasReleaseSigningConfig) {
                signingConfigs["release"]
            } else {
                signingConfigs["debug"]
            }
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

if (releaseTasksRequested && !hasReleaseSigningConfig) {
    throw GradleException(
        "Release signing configuration is incomplete. Missing: "
                + releaseSigningMissingKeys.joinToString(", ")
    )
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

