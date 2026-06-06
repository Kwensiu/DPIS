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
        List<QuickTemplateTargetsBinder.TargetAppItem> installed = List.of(
                new QuickTemplateTargetsBinder.TargetAppItem(
                        "Installed", "com.example.installed", false),
                new QuickTemplateTargetsBinder.TargetAppItem(
                        "Other", "com.example.other", true));

        LinkedHashSet<String> pruned =
                QuickTemplateTargetsBinder.pruneSelectedPackagesToInstalledApps(
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
        QuickTemplateTargetsBinder.pruneSelectedPackagesToInstalledApps(
                selectedPackages,
                List.of(new QuickTemplateTargetsBinder.TargetAppItem(
                        "Installed", "com.example.installed", false)));

        assertTrue(store.setSelectedPackages("template_a", selectedPackages));

        assertEquals(orderedSet("com.example.installed"),
                store.read("template_a").selectedPackages);
        assertFalse(prefs.contains(DpiConfigStore.KEY_TARGET_PACKAGES));
    }

    @Test
    public void targetFiltersHideConfiguredAppsOnlyWhenEnabled() {
        QuickTemplateTargetsBinder.TargetAppItem configured =
                new QuickTemplateTargetsBinder.TargetAppItem(
                        "Configured", "com.example.configured", true);
        QuickTemplateTargetsBinder.TargetAppItem unconfigured =
                new QuickTemplateTargetsBinder.TargetAppItem(
                        "Plain", "com.example.plain", false);

        assertTrue(QuickTemplateTargetsBinder.matchesTargetFilters(
                configured, "", false, false));
        assertTrue(QuickTemplateTargetsBinder.matchesTargetFilters(
                unconfigured, "", false, false));
        assertFalse(QuickTemplateTargetsBinder.matchesTargetFilters(
                configured, "", false, true));
        assertTrue(QuickTemplateTargetsBinder.matchesTargetFilters(
                unconfigured, "", false, true));
    }

    @Test
    public void targetFiltersHideSystemAppsByDefault() {
        QuickTemplateTargetsBinder.TargetAppItem system =
                new QuickTemplateTargetsBinder.TargetAppItem(
                        "System", "android", false, true, null);

        assertFalse(QuickTemplateTargetsBinder.matchesTargetFilters(
                system, "", false, false));
        assertTrue(QuickTemplateTargetsBinder.matchesTargetFilters(
                system, "", true, false));
    }

    private static LinkedHashSet<String> orderedSet(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }
}
