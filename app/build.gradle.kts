import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.aboutlibraries)
}

private val appVersionName = "1.6.2" // x-release-please-version

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

abstract class RenameReleaseApkTask : DefaultTask() {
    @get:Internal
    abstract val sourceApk: RegularFileProperty

    @get:OutputFile
    abstract val targetApk: RegularFileProperty

    @TaskAction
    fun renameApk() {
        val source = sourceApk.get().asFile
        val target = targetApk.get().asFile

        if (!source.exists()) {
            if (target.exists()) {
                logger.lifecycle("Release APK already renamed: ${target.absolutePath}")
                return
            }
            throw GradleException("Release APK not found: ${source.absolutePath}")
        }

        target.parentFile?.mkdirs()
        if (target.exists() && !target.delete()) {
            throw GradleException("Failed to replace existing APK: ${target.absolutePath}")
        }

        source.copyTo(target, overwrite = true)
        logger.lifecycle("Release APK copied to: ${target.name}")
    }
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

val renamedReleaseApkName = "DPIS_${appVersionName}.apk"

val renameReleaseApk = tasks.register("renameReleaseApk", RenameReleaseApkTask::class) {
    sourceApk.set(layout.buildDirectory.file("outputs/apk/release/app-release.apk"))
    targetApk.set(layout.buildDirectory.file("outputs/apk/release/$renamedReleaseApkName"))
}

tasks.configureEach {
    if (name == "assembleRelease") {
        finalizedBy(renameReleaseApk)
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

