package com.dpis.module.hyperos

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HyperOsRustProcessArgPolicyTest {
    @Test
    fun parseRequiresEnvironmentsSlotAndStringPackage() {
        assertNull(HyperOsRustProcessArgPolicy.parse(null))
        assertNull(HyperOsRustProcessArgPolicy.parse(listOf("ignored", "com.miui.gallery")))
        assertNull(HyperOsRustProcessArgPolicy.parse(startArgs(packageName = 1)))

        val parsed = HyperOsRustProcessArgPolicy.parse(
            startArgs(
                packageName = "com.miui.gallery",
                binaryPath = "/data/app/libapp.so",
                environments = "EXISTING=value",
            ),
        )
        assertNotNull(parsed)
        assertEquals("com.miui.gallery", parsed!!.packageName)
        assertEquals("/data/app/libapp.so", parsed.binaryPath)
        assertEquals("EXISTING=value", parsed.existingEnvironments)
    }

    @Test
    fun parseTreatsNonStringBinaryAndEnvAsEmpty() {
        val parsed = HyperOsRustProcessArgPolicy.parse(
            startArgs(binaryPath = 20, environments = 21),
        )
        assertNotNull(parsed)
        assertEquals("", parsed!!.binaryPath)
        assertEquals("", parsed.existingEnvironments)
    }

    @Test
    fun shouldRewriteRequiresEnabledHookAndPositiveFontScale() {
        assertFalse(HyperOsRustProcessArgPolicy.shouldRewrite(300, false))
        assertFalse(HyperOsRustProcessArgPolicy.shouldRewrite(null, true))
        assertFalse(HyperOsRustProcessArgPolicy.shouldRewrite(0, true))
        assertTrue(HyperOsRustProcessArgPolicy.shouldRewrite(300, true))
    }

    @Test
    fun appendEnvironmentIncludesFontTargetAndJoinsExisting() {
        val empty = HyperOsRustProcessArgPolicy.appendEnvironment(
            "",
            "com.miui.gallery",
            300,
            "/data/app/MIUIGallery/lib/arm64/libapp_gallery.so",
        )
        assertEquals(
            "DPIS_PACKAGE=com.miui.gallery --envs=DPIS_FONT_SCALE_PERCENT=300" +
                " --envs=DPIS_RUST_BINARY=/data/app/MIUIGallery/lib/arm64/libapp_gallery.so" +
                " --envs=DPIS_NATIVE_ROUTE=PARAGRAPH_BUILDER" +
                " --cold-boot-speed",
            empty,
        )

        val joined = HyperOsRustProcessArgPolicy.appendEnvironment(
            "FOO=bar",
            "com.miui.gallery",
            180,
            "/data/app/libapp.so",
        )
        assertTrue(joined.startsWith("FOO=bar, --envs=DPIS_PACKAGE=com.miui.gallery"))
        assertTrue(joined.contains("--envs=DPIS_FONT_SCALE_PERCENT=180"))
        assertTrue(joined.contains("--envs=DPIS_NATIVE_ROUTE=PARAGRAPH_BUILDER"))
        assertTrue(joined.endsWith(" --cold-boot-speed"))

        val weather = HyperOsRustProcessArgPolicy.appendEnvironment(
            "",
            "com.miui.weather2",
            200,
            "/data/app/libweather_app.so",
        )
        assertTrue(weather.contains("--envs=DPIS_NATIVE_ROUTE=CONFIGURATION_GOT"))
        val unknown = HyperOsRustProcessArgPolicy.appendEnvironment(
            "",
            "com.example.app",
            200,
            "/data/app/libapp.so",
        )
        assertFalse(unknown.contains("DPIS_NATIVE_ROUTE"))
    }

    @Test
    fun appendEnvironmentSanitizesCommaNewlineAndSpace() {
        val envs = HyperOsRustProcessArgPolicy.appendEnvironment(
            "",
            "com.miui.gallery, x",
            100,
            "a b\nc",
        )
        assertTrue(envs.contains("DPIS_PACKAGE=com.miui.gallery__x"))
        assertTrue(envs.contains("DPIS_RUST_BINARY=a_b_c"))
    }

    @Test
    fun withProxyAndEnvironmentWritesBinaryAndEnvSlots() {
        val args = startArgs().toMutableList()
        val updated = HyperOsRustProcessArgPolicy.withProxyAndEnvironment(
            args,
            "/data/app/libdpis_native.so",
            "DPIS_PACKAGE=com.miui.gallery --cold-boot-speed",
        )
        assertEquals("/data/app/libdpis_native.so", updated[HyperOsRustProcessArgPolicy.BINARY_PATH_INDEX])
        assertEquals(
            "DPIS_PACKAGE=com.miui.gallery --cold-boot-speed",
            updated[HyperOsRustProcessArgPolicy.ENVIRONMENTS_INDEX],
        )
        assertEquals("com.miui.gallery", updated[HyperOsRustProcessArgPolicy.PACKAGE_NAME_INDEX])
    }

    @Test
    fun argumentProbeSummarizesGalleryOrWeatherAndSkipsOthers() {
        val weather = HyperOsRustProcessArgPolicy.buildArgumentProbeSummary(
            listOf(
                "ignored",
                "com.miui.weather2",
                1,
                "/data/app/weather/lib/arm64/libweather_app.so",
                "--envs=EXISTING=value",
            ),
        )
        assertNotNull(weather)
        assertTrue(weather!!.contains("size=5"))
        assertTrue(weather.contains("1=com.miui.weather2"))
        assertTrue(weather.contains("3=/data/app/weather/lib/arm64/libweather_app.so"))
        assertTrue(weather.contains("4=--envs=EXISTING=value"))

        assertNull(
            HyperOsRustProcessArgPolicy.buildArgumentProbeSummary(
                listOf("com.example.app", "/data/app/example/libfoo.so"),
            ),
        )
    }

    private fun startArgs(
        packageName: Any? = "com.miui.gallery",
        binaryPath: Any? = "/data/app/libapp.so",
        environments: Any? = "",
    ): List<Any?> {
        val args = MutableList<Any?>(HyperOsRustProcessArgPolicy.ENVIRONMENTS_INDEX + 1) { null }
        args[0] = "ignored"
        args[HyperOsRustProcessArgPolicy.PACKAGE_NAME_INDEX] = packageName
        args[HyperOsRustProcessArgPolicy.BINARY_PATH_INDEX] = binaryPath
        args[HyperOsRustProcessArgPolicy.ENVIRONMENTS_INDEX] = environments
        return args
    }
}
