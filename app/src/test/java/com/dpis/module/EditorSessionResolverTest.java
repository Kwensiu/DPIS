package com.dpis.module;
import com.dpis.module.appconfig.EditorDraft;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.dpis.module.appconfig.EditorSessionResolver;
import com.dpis.module.applist.AppListItem;
import com.dpis.module.fonts.FontApplyMode;
import com.dpis.module.viewport.ViewportApplyMode;
import com.dpis.module.viewport.ViewportTargetSpec;

import java.util.List;

import org.junit.Test;

public final class EditorSessionResolverTest {
    @Test
    public void initializesDraftOnlyWhenPackageChanges() {
        AppListItem item = app("com.example.app");
        EditorSessionResolver.Session initialized = EditorSessionResolver.resolve(item, null, null);
        EditorDraft current = initialized.draft.withFontInput("130");
        EditorDraft saved = initialized.savedDraft;

        EditorSessionResolver.Session retained = EditorSessionResolver.resolve(item, current, saved);

        assertTrue(initialized.initialized);
        assertTrue(!retained.initialized);
        assertSame(current, retained.draft);
        assertSame(saved, retained.savedDraft);
    }

    @Test
    public void findsOnlyTheRequestedPackage() {
        AppListItem item = app("com.example.app");
        assertSame(item, EditorSessionResolver.findItem(
                List.of(item), "com.example.app"));
    }

    private static AppListItem app(String packageName) {
        return new AppListItem(
                "Example", packageName, true, true, null, null,
                ViewportApplyMode.OFF, null, ViewportTargetSpec.off(), null,
                FontApplyMode.OFF, null, false, null, true, false, true, false,
                false, null);
    }
}
