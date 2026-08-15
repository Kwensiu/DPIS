package com.dpis.module;

import com.dpis.module.runtime.font.TypefaceOverrideHookInstaller;

import com.dpis.module.fonts.PublishedFontFileResolver;

import com.dpis.module.fonts.FontTypefaceLoader;

import android.graphics.Typeface;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class TypefaceOverrideHookInstallerTest {
    @Test
    public void explicitStyleWinsWhenPresent() {
        int style = TypefaceOverrideHookInstaller.resolveStyleForTest(Typeface.ITALIC, Typeface.BOLD);

        assertEquals(Typeface.BOLD, style);
    }

    @Test
    public void originalTypefaceStyleIsUsedWhenExplicitStyleMissing() {
        int style = TypefaceOverrideHookInstaller.resolveStyleForTest(Typeface.BOLD_ITALIC, null);

        assertEquals(Typeface.BOLD_ITALIC, style);
    }

    @Test
    public void normalStyleIsUsedWhenNoStyleExists() {
        int style = TypefaceOverrideHookInstaller.resolveStyleForTest(null, null);

        assertEquals(Typeface.NORMAL, style);
    }

    @Test
    public void nullBaseTypefaceReturnsOriginalTypeface() {
        Typeface result = TypefaceOverrideHookInstaller.resolveReplacementForTest(null, Typeface.DEFAULT_BOLD);

        assertEquals(Typeface.DEFAULT_BOLD, result);
    }

    @Test
    public void replacementStylePrefersExplicitStyle() {
        int style = TypefaceOverrideHookInstaller.resolveReplacementStyleForTest(
                Typeface.BOLD,
                Typeface.ITALIC);

        assertEquals(Typeface.ITALIC, style);
    }

    @Test
    public void replacementStyleUsesNormalWhenOriginalMissing() {
        int style = TypefaceOverrideHookInstaller.resolveReplacementStyleForTest(
                null,
                null);

        assertEquals(Typeface.NORMAL, style);
    }

    @Test
    public void modernInstallerFallsBackToPublishedFontFile() throws Exception {
        String source = SourceSmokeTestPaths.read("src/main/java/com/dpis/module/runtime/font/TypefaceOverrideHookInstaller.java");

        assertTrue(source.contains("PublishedFontFileResolver.resolve(typefaceId)"));
        assertTrue(source.contains("SystemFontRegistry.loadTypeface(typefaceId)"));
        assertTrue(source.contains("fontLibraryStore.findById(typefaceId)"));
        assertTrue(source.contains("FontProviderTypefaceLoader.load(typefaceId, ttcIndex)"));
        assertTrue(source.contains("FontTypefaceLoader.load(file, ttcIndex)"));
    }

    @Test
    public void modernInstallerAcceptsResolvedPlanTypefaceId() throws Exception {
        String source = SourceSmokeTestPaths.read("src/main/java/com/dpis/module/runtime/font/TypefaceOverrideHookInstaller.java");

        assertTrue(source.contains("String targetTypefaceId"));
        assertTrue(source.contains("String typefaceId = targetTypefaceId"));
        assertTrue(source.contains("store.getTargetTypefaceId(packageName)"));
        assertTrue(source.contains("target typeface loaded"));
        assertTrue(source.contains("system typeface unavailable"));
    }

    @Test
    public void modernInstallerGuardIsScopedToCurrentProcess() throws Exception {
        String source = SourceSmokeTestPaths.read("src/main/java/com/dpis/module/runtime/font/TypefaceOverrideHookInstaller.java");

        assertTrue(source.contains("installedPid"));
        assertTrue(source.contains("ProcessScopedInstallGate.isInstalledForCurrentProcess"));
        assertTrue(source.contains("ProcessScopedInstallGate.currentPid()"));
    }

    @Test
    public void modernInstallerLogsFirstReplacementHits() throws Exception {
        String source = SourceSmokeTestPaths.read("src/main/java/com/dpis/module/runtime/font/TypefaceOverrideHookInstaller.java");

        assertTrue(source.contains("replacement hit: package="));
        assertTrue(source.contains("TextView.setTypeface(Typeface)"));
        assertTrue(source.contains("TextView.setTypeface(Typeface,int)"));
        assertTrue(source.contains("Paint.setTypeface"));
    }

    @Test
    public void modernInstallerRecordsStableTypefaceDiagnosticStages() throws Exception {
        String source = SourceSmokeTestPaths.read("src/main/java/com/dpis/module/runtime/font/TypefaceOverrideHookInstaller.java");

        assertTrue(source.contains("RuntimeEvents.recordTypeface("));
        assertTrue(source.contains("\"source_provider_loaded\""));
        assertTrue(source.contains("\"source_fallback_loaded\""));
        assertTrue(source.contains("\"hook_installed\""));
        assertTrue(source.contains("\"replacement_hit\""));
        assertTrue(source.contains("\"load_failed\""));
    }

    @Test
    public void modernInstallerRecordsLoadSourceAndTtcIndex() throws Exception {
        String source = SourceSmokeTestPaths.read("src/main/java/com/dpis/module/runtime/font/TypefaceOverrideHookInstaller.java");

        assertTrue(source.contains("\"load_source\""));
        assertTrue(source.contains("source=\" + source"));
        assertTrue(source.contains("ttcIndex=\" + ttcIndex"));
        assertTrue(source.contains("loadSource=\" + loadSourceFor"));
        assertTrue(source.contains("ttcIndex=\" + ttcIndexFor"));
    }

    @Test
    public void modernTypefaceEventsReachRuntimeTransport() throws Exception {
        String source = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/diagnostics/RuntimeEvents.java");

        assertTrue(source.contains("RuntimeTransport.record("));
        assertTrue(source.contains("\"typeface\", stage, packageName, message"));
    }

    @Test
    public void modernInstallerAppliesTypefaceWhenTextViewAttaches() throws Exception {
        String source = SourceSmokeTestPaths.read("src/main/java/com/dpis/module/runtime/font/TypefaceOverrideHookInstaller.java");

        assertTrue(source.contains("installTextViewAttachHook("));
        assertTrue(source.contains("getDeclaredMethod(\"onAttachedToWindow\")"));
        assertTrue(source.contains("return View.class.getDeclaredMethod(\"onAttachedToWindow\")"));
        assertTrue(source.contains("TextView.onAttachedToWindow"));
        assertTrue(source.contains("TextView attach hook ready"));
    }

    @Test
    public void modernInstallerAppliesTypefaceWhenTextViewDraws() throws Exception {
        String source = SourceSmokeTestPaths.read("src/main/java/com/dpis/module/runtime/font/TypefaceOverrideHookInstaller.java");

        assertTrue(source.contains("installTextViewDrawHook("));
        assertTrue(source.contains("getDeclaredMethod(\"onDraw\", Canvas.class)"));
        assertTrue(source.contains("TextView.onDraw"));
        assertTrue(source.contains("TextView draw hook ready"));
    }

    @Test
    public void parseTtcIndexFromIdExtractsIndex() {
        assertEquals(2, TypefaceOverrideHookInstaller.parseTtcIndexFromIdForTest("font_abcd1234_ttc_2"));
        assertEquals(127, TypefaceOverrideHookInstaller.parseTtcIndexFromIdForTest("font_abcd1234_ttc_127"));
        assertEquals(0, TypefaceOverrideHookInstaller.parseTtcIndexFromIdForTest("font_abcd1234"));
        assertEquals(0, TypefaceOverrideHookInstaller.parseTtcIndexFromIdForTest("font_abcd1234_ttc_"));
        assertEquals(0, TypefaceOverrideHookInstaller.parseTtcIndexFromIdForTest("font_abcd1234_ttc_-1"));
    }

}
