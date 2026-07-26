import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.aboutlibraries)
    alias(libs.plugins.androidx.baselineprofile)
}

private val appVersionName = "1.15.0" // x-release-please-version
private val defaultNdkVersion = "28.2.13676358"
private val defaultCmakeVersion = "3.22.1"

private fun readGradleOrEnv(name: String): String? {
    val gradleValue = project.findProperty(name)?.toString()?.trim()
    if (!gradleValue.isNullOrEmpty()) {
        return gradleValue
    }
    val envValue = System.getenv(name)?.trim()
    return if (envValue.isNullOrEmpty()) null else envValue
}

private fun readGradleOrEnvInt(name: String): Int? {
    val value = readGradleOrEnv(name) ?: return null
    return value.toIntOrNull()
        ?: throw GradleException("$name must be an integer: $value")
}

private fun readNativeToolchainOverride(name: String): String? {
    val gradleValue = project.findProperty(name)?.toString()?.trim()
    val envValue = System.getenv(name)?.trim()
    if (!gradleValue.isNullOrEmpty() && !envValue.isNullOrEmpty() && gradleValue != envValue) {
        throw GradleException(
            "$name is set by both Gradle property and environment variable with different values."
        )
    }
    return when {
        !gradleValue.isNullOrEmpty() -> gradleValue
        !envValue.isNullOrEmpty() -> envValue
        else -> null
    }
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
    val taskName = it.substringAfterLast(":")
    taskName == "renameReleaseApk" || taskName.endsWith("Release")
}
private val versionNameOverride = readGradleOrEnv("DPIS_VERSION_NAME")
private val versionCodeOverride = readGradleOrEnvInt("DPIS_VERSION_CODE")
private val resolvedNdkVersion = readNativeToolchainOverride("DPIS_NDK_VERSION") ?: defaultNdkVersion
private val resolvedCmakeVersion = readNativeToolchainOverride("DPIS_CMAKE_VERSION") ?: defaultCmakeVersion
private val cmakeCompilerLauncher = readNativeToolchainOverride("DPIS_CMAKE_COMPILER_LAUNCHER")
private val cmakeMakeProgram = readNativeToolchainOverride("DPIS_CMAKE_MAKE_PROGRAM")
if ((versionNameOverride == null) != (versionCodeOverride == null)) {
    throw GradleException("DPIS_VERSION_NAME and DPIS_VERSION_CODE must be set together.")
}
private val resolvedVersionName = versionNameOverride ?: appVersionName
private val resolvedVersionCode = versionCodeOverride ?: semVerToVersionCode(appVersionName)

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
    // Keep SDK, Java, packaging, and flavor conventions local while DPIS remains
    // a single-module app. If more Android modules appear, move these shared
    // defaults into a small convention plugin instead of duplicating them.
    namespace = "com.dpis.module"
    // Compose Material3 1.5 alpha is built against API 37; runtime targeting remains API 36.
    compileSdk = 37
    buildToolsVersion = "36.1.0"
    ndkVersion = resolvedNdkVersion

    flavorDimensions += "xposedApi"

    defaultConfig {
        applicationId = "io.github.kwensiu.dpis"
        minSdk = 26
        targetSdk = 36
        versionName = resolvedVersionName
        versionCode = resolvedVersionCode
        testInstrumentationRunner = "com.dpis.module.DpisTestRunner"
        buildConfigField(
            "String",
            "GITHUB_RELEASES_API_URL",
            "\"https://api.github.com/repos/Kwensiu/DPIS/releases\""
        )

        externalNativeBuild {
            cmake {
                arguments += "-DANDROID_STL=c++_static"
                if (cmakeCompilerLauncher != null) {
                    arguments += "-DCMAKE_C_COMPILER_LAUNCHER=$cmakeCompilerLauncher"
                    arguments += "-DCMAKE_CXX_COMPILER_LAUNCHER=$cmakeCompilerLauncher"
                }
                if (cmakeMakeProgram != null) {
                    arguments += "-DCMAKE_MAKE_PROGRAM=$cmakeMakeProgram"
                }
            }
        }
    }

    productFlavors {
        create("modern") {
            dimension = "xposedApi"
        }
        create("legacy") {
            dimension = "xposedApi"
        }
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
        jniLibs {
            useLegacyPackaging = true
        }
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
        compose = true
    }

    lint {
        lintConfig = file("lint.xml")
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = resolvedCmakeVersion
        }
    }
}

abstract class SyncNativeProxyAssetTask : DefaultTask() {
    @get:InputFiles
    abstract val sourceFiles: ConfigurableFileTree

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun syncNativeProxyAsset() {
        val output = outputDirectory.get().asFile
        if (output.exists()) {
            output.deleteRecursively()
        }
        sourceFiles.files.forEach { source ->
            val abi = source.parentFile.name
            val target = output.resolve("native/$abi/libdpis_native.so")
            target.parentFile.mkdirs()
            source.copyTo(target, overwrite = true)
        }
    }
}

androidComponents {
    onVariants(selector().all()) { variant ->
        val capitalizedName = variant.name.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase() else char.toString()
        }
        val syncNativeProxy = tasks.register<SyncNativeProxyAssetTask>(
            "sync${capitalizedName}NativeProxyAsset"
        ) {
            sourceFiles.setDir(layout.buildDirectory.dir("intermediates/cxx/${variant.name}"))
            sourceFiles.include("**/obj/*/libdpis_native.so")
            outputDirectory.set(layout.buildDirectory.dir("generated/assets/nativeProxy/${variant.name}"))
            dependsOn("externalNativeBuild${capitalizedName}")
        }
        variant.sources.assets?.addGeneratedSourceDirectory(syncNativeProxy) { it.outputDirectory }
    }
}

val renamedReleaseApkName = "DPIS_${resolvedVersionName}.apk"
val renamedLegacyApkName = "DPIS_${resolvedVersionName}_legacy.apk"

val renameReleaseApk = tasks.register("renameReleaseApk", RenameReleaseApkTask::class) {
    sourceApk.set(layout.buildDirectory.file("outputs/apk/modern/release/app-modern-release.apk"))
    targetApk.set(layout.buildDirectory.file("outputs/apk/modern/release/$renamedReleaseApkName"))
}

val renameLegacyApk = tasks.register("renameLegacyApk", RenameReleaseApkTask::class) {
    sourceApk.set(layout.buildDirectory.file("outputs/apk/legacy/release/app-legacy-release.apk"))
    targetApk.set(layout.buildDirectory.file("outputs/apk/legacy/release/$renamedLegacyApkName"))
}

tasks.register<Test>("testDebugUnitTest") {
    val modernTest = tasks.named<Test>("testModernDebugUnitTest").get()
    description = "Compatibility alias for filtered debug unit tests; runs modern debug unit tests."
    group = "verification"
    testClassesDirs = modernTest.testClassesDirs
    classpath = modernTest.classpath
    shouldRunAfter(modernTest)
}

tasks.register("testAllDebugUnitTests") {
    dependsOn("testModernDebugUnitTest", "testLegacyDebugUnitTest")
}

tasks.configureEach {
    if (name == "assembleModernRelease" || name == "assembleRelease") {
        finalizedBy(renameReleaseApk)
    }
    if (name == "assembleLegacyRelease" || name == "assembleRelease") {
        finalizedBy(renameLegacyApk)
    }
}

if (releaseTasksRequested && !hasReleaseSigningConfig) {
    throw GradleException(
        "Release signing configuration is incomplete. Missing: "
                + releaseSigningMissingKeys.joinToString(", ")
    )
}

dependencies {
    baselineProfile(project(":baselineprofile"))
    compileOnly(libs.libxposed.api)
    compileOnly(libs.legacy.xposed.api)
    implementation(libs.libxposed.service)
    implementation(libs.dexkit)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.material)
    implementation(libs.markwon.core)
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    testImplementation(libs.junit4)
    testRuntimeOnly(libs.legacy.xposed.api)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
}
