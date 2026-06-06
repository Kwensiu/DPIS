package com.dpis.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

public class QuickTemplateTargetSelectionTest {
    @Test
    public void pruneSelectedPackagesKeepsOnlyInstalledApps() {
        LinkedHashSet<String> selectedPackages = orderedSet(
                "com.example.installed",
                "com.example.removed",
                " com.example.other ");
        List<QuickTemplateTargetSelectionActivity.TargetAppItem> installed = List.of(
                new QuickTemplateTargetSelectionActivity.TargetAppItem(
                        "Installed", "com.example.installed", false),
                new QuickTemplateTargetSelectionActivity.TargetAppItem(
                        "Other", "com.example.other", true));

        LinkedHashSet<String> pruned =
                QuickTemplateTargetSelectionActivity.pruneSelectedPackagesToInstalledApps(
                        selectedPackages, installed);

        assertEquals(orderedSet("com.example.installed", "com.example.other"), pruned);
        assertEquals(pruned, selectedPackages);
    }

    @Test
    public void savingPrunedSelectionTouchesTemplateSelectionOnly() {
        FakePrefs prefs = new FakePrefs();
        QuickTemplateStore store = new QuickTemplateStore(prefs);
        assertTrue(store.save(new QuickTemplateStore.QuickTemplate(
                "template_a",
                "Compact",
                1L,
                orderedSet("com.example.installed", "com.example.removed"),
                TemplateConfigValue.EMPTY)));

        LinkedHashSet<String> selectedPackages = new LinkedHashSet<>(
                store.read("template_a").selectedPackages);
        QuickTemplateTargetSelectionActivity.pruneSelectedPackagesToInstalledApps(
                selectedPackages,
                List.of(new QuickTemplateTargetSelectionActivity.TargetAppItem(
                        "Installed", "com.example.installed", false)));

        assertTrue(store.setSelectedPackages("template_a", selectedPackages));

        assertEquals(orderedSet("com.example.installed"),
                store.read("template_a").selectedPackages);
        assertFalse(prefs.contains(DpiConfigStore.KEY_TARGET_PACKAGES));
    }

    @Test
    public void targetFiltersHideConfiguredAppsOnlyWhenEnabled() {
        QuickTemplateTargetSelectionActivity.TargetAppItem configured =
                new QuickTemplateTargetSelectionActivity.TargetAppItem(
                        "Configured", "com.example.configured", true);
        QuickTemplateTargetSelectionActivity.TargetAppItem unconfigured =
                new QuickTemplateTargetSelectionActivity.TargetAppItem(
                        "Plain", "com.example.plain", false);

        assertTrue(QuickTemplateTargetSelectionActivity.matchesTargetFilters(
                configured, "", false, false));
        assertTrue(QuickTemplateTargetSelectionActivity.matchesTargetFilters(
                unconfigured, "", false, false));
        assertFalse(QuickTemplateTargetSelectionActivity.matchesTargetFilters(
                configured, "", false, true));
        assertTrue(QuickTemplateTargetSelectionActivity.matchesTargetFilters(
                unconfigured, "", false, true));
    }

    @Test
    public void targetFiltersHideSystemAppsByDefault() {
        QuickTemplateTargetSelectionActivity.TargetAppItem system =
                new QuickTemplateTargetSelectionActivity.TargetAppItem(
                        "System", "android", false, true, null);

        assertFalse(QuickTemplateTargetSelectionActivity.matchesTargetFilters(
                system, "", false, false));
        assertTrue(QuickTemplateTargetSelectionActivity.matchesTargetFilters(
                system, "", true, false));
    }

    private static LinkedHashSet<String> orderedSet(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }
}
