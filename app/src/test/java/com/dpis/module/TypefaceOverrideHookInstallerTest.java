package com.dpis.module;

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
        String source = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of("src/main/java/com/dpis/module/TypefaceOverrideHookInstaller.java")),
                java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(source.contains("PublishedFontFileResolver.resolve(typefaceId)"));
        assertTrue(source.contains("SystemFontRegistry.loadTypeface(typefaceId)"));
        assertTrue(source.contains("fontLibraryStore.findById(typefaceId)"));
        assertTrue(source.contains("FontTypefaceLoader.load(file, ttcIndex)"));
    }

    @Test
    public void modernInstallerAcceptsResolvedPlanTypefaceId() throws Exception {
        String source = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of("src/main/java/com/dpis/module/TypefaceOverrideHookInstaller.java")),
                java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(source.contains("String targetTypefaceId"));
        assertTrue(source.contains("String typefaceId = targetTypefaceId"));
        assertTrue(source.contains("store.getTargetTypefaceId(packageName)"));
        assertTrue(source.contains("target typeface loaded"));
        assertTrue(source.contains("system typeface unavailable"));
    }

    @Test
    public void modernInstallerGuardIsScopedToCurrentProcess() throws Exception {
        String source = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of("src/main/java/com/dpis/module/TypefaceOverrideHookInstaller.java")),
                java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(source.contains("hookInstalledPid"));
        assertTrue(source.contains("Process.myPid()"));
        assertTrue(source.contains("isHookInstalledForCurrentProcess()"));
    }

    @Test
    public void modernInstallerLogsFirstReplacementHits() throws Exception {
        String source = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of("src/main/java/com/dpis/module/TypefaceOverrideHookInstaller.java")),
                java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(source.contains("replacement hit: package="));
        assertTrue(source.contains("TextView.setTypeface(Typeface)"));
        assertTrue(source.contains("TextView.setTypeface(Typeface,int)"));
        assertTrue(source.contains("Paint.setTypeface"));
    }

    @Test
    public void modernInstallerAppliesTypefaceWhenTextViewAttaches() throws Exception {
        String source = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of("src/main/java/com/dpis/module/TypefaceOverrideHookInstaller.java")),
                java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(source.contains("installTextViewAttachHook("));
        assertTrue(source.contains("getDeclaredMethod(\"onAttachedToWindow\")"));
        assertTrue(source.contains("return View.class.getDeclaredMethod(\"onAttachedToWindow\")"));
        assertTrue(source.contains("TextView.onAttachedToWindow"));
        assertTrue(source.contains("TextView attach hook ready"));
    }

    @Test
    public void modernInstallerAppliesTypefaceWhenTextViewDraws() throws Exception {
        String source = new String(java.nio.file.Files.readAllBytes(
                java.nio.file.Path.of("src/main/java/com/dpis/module/TypefaceOverrideHookInstaller.java")),
                java.nio.charset.StandardCharsets.UTF_8);

        assertTrue(source.contains("installTextViewDrawHook("));
        assertTrue(source.contains("getDeclaredMethod(\"onDraw\", Canvas.class)"));
        assertTrue(source.contains("TextView.onDraw"));
        assertTrue(source.contains("TextView draw hook ready"));
    }

}
