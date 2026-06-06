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

    private static LinkedHashSet<String> orderedSet(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }
}
