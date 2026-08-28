package com.dpis.module.templates;

import android.content.SharedPreferences;
import android.content.Context;
import com.dpis.module.DpisConfigStore;
import com.dpis.module.backup.TemplateBackup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Map;

public final class QuickTemplateStore {
    public static final String KEY_TEMPLATE_IDS = "template.ids";
    public static final String KEY_TEMPLATE_ORDER = "template.order";
    public static final String GROUP = "quick_templates";

    private final SharedPreferences preferences;

    public QuickTemplateStore(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    /** Owns template data outside the runtime configuration preference group. */
    public QuickTemplateStore(Context context) {
        this.preferences = context.getSharedPreferences(GROUP, Context.MODE_PRIVATE);
        migrateLegacyTemplates(context.getSharedPreferences(DpisConfigStore.GROUP, Context.MODE_PRIVATE));
    }

    private void migrateLegacyTemplates(SharedPreferences legacy) {
        if (!preferences.getAll().isEmpty() || legacy == null) return;
        SharedPreferences.Editor editor = preferences.edit();
        boolean found = false;
        for (Map.Entry<String, ?> entry : legacy.getAll().entrySet()) {
            if (entry.getKey().startsWith("template.")) {
                putTypedValue(editor, entry.getKey(), entry.getValue());
                found = true;
            }
        }
        if (found && editor.commit()) {
            SharedPreferences.Editor cleanup = legacy.edit();
            for (String key : legacy.getAll().keySet()) {
                if (key.startsWith("template.")) cleanup.remove(key);
            }
            cleanup.commit();
        }
    }

    public void copyToBackup(Map<String, Object> entries) {
        if (entries == null) return;
        for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            if (entry.getKey().startsWith("template.")) {
                Object value = entry.getValue();
                if (value instanceof Set) entries.put(entry.getKey(), new LinkedHashSet<>((Set<String>) value));
                else entries.put(entry.getKey(), value);
            }
        }
    }

    public boolean restoreFromBackup(Map<String, Object> entries) {
        if (!containsTemplateEntries(entries)) return false;
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : preferences.getAll().keySet()) if (key.startsWith("template.")) editor.remove(key);
        for (Map.Entry<String, Object> entry : entries.entrySet()) {
            if (entry.getKey().startsWith("template.")) putTypedValue(editor, entry.getKey(), entry.getValue());
        }
        return editor.commit();
    }

    /** Exports templates as domain values; preference key layout stays private to this store. */
    public List<TemplateBackup> exportBackup() {
        List<TemplateBackup> result = new ArrayList<>();
        for (QuickTemplate template : readAll()) {
            result.add(new TemplateBackup(template.id, template.name, template.updatedAt,
                    template.selectedPackages, template.configValue));
        }
        return result;
    }

    /** Restores the complete ordered template catalog from domain values. */
    public boolean restoreBackup(List<TemplateBackup> backups) {
        if (backups == null) return false;
        SharedPreferences.Editor editor = preferences.edit();
        for (String key : preferences.getAll().keySet()) {
            if (key.startsWith("template.")) editor.remove(key);
        }
        if (!editor.commit()) return false;
        for (TemplateBackup backup : backups) {
            if (backup == null || !save(new QuickTemplate(backup.id, backup.name,
                    backup.updatedAt, backup.selectedPackages, backup.configValue))) {
                return false;
            }
        }
        List<String> orderedIds = new ArrayList<>();
        for (TemplateBackup backup : backups) {
            if (backup != null) orderedIds.add(backup.id);
        }
        return reorder(orderedIds);
    }

    /**
     * A backup from before template storage was introduced has no template
     * entries. Such a backup must not delete the user's current local catalog.
     */
    public static boolean containsTemplateEntries(Map<String, ?> entries) {
        if (entries == null) return false;
        for (String key : entries.keySet()) {
            if (key != null && key.startsWith("template.")) return true;
        }
        return false;
    }

    private static void putTypedValue(SharedPreferences.Editor editor, String key, Object value) {
        if (value instanceof String) editor.putString(key, (String) value);
        else if (value instanceof Boolean) editor.putBoolean(key, (Boolean) value);
        else if (value instanceof Integer) editor.putInt(key, (Integer) value);
        else if (value instanceof Long) editor.putLong(key, (Long) value);
        else if (value instanceof Float) editor.putFloat(key, (Float) value);
        else if (value instanceof Set) editor.putStringSet(key, new LinkedHashSet<>((Set<String>) value));
    }

    public String newTemplateId() {
        return UUID.randomUUID().toString();
    }

    public List<QuickTemplate> readAll() {
        ArrayList<QuickTemplate> templates = new ArrayList<>();
        for (String id : getTemplateIds()) {
            QuickTemplate template = read(id);
            if (template != null) {
                templates.add(template);
            }
        }
        List<String> orderedIds = getTemplateOrderIds();
        if (orderedIds.isEmpty()) {
            templates.sort(defaultComparator());
            return templates;
        }
        templates.sort((left, right) -> {
            int leftIndex = orderedIds.indexOf(left.id);
            int rightIndex = orderedIds.indexOf(right.id);
            boolean leftOrdered = leftIndex >= 0;
            boolean rightOrdered = rightIndex >= 0;
            if (leftOrdered && rightOrdered) {
                return Integer.compare(leftIndex, rightIndex);
            }
            if (leftOrdered) {
                return -1;
            }
            if (rightOrdered) {
                return 1;
            }
            return defaultComparator().compare(left, right);
        });
        return templates;
    }

    public QuickTemplate read(String id) {
        String normalizedId = normalizeId(id);
        if (normalizedId == null) {
            return null;
        }
        String prefix = prefixFor(normalizedId);
        String name = getString(prefix + "name", null);
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        long updatedAt = getLong(prefix + "updated_at", 0L);
        return new QuickTemplate(
                normalizedId,
                name.trim(),
                updatedAt,
                getSelectedPackages(normalizedId),
                TemplateCustomSemantics.customValue(
                        TemplateConfigPreferences.read(preferences, prefix + "config.")));
    }

    public boolean hasDuplicateName(String name, String excludedId) {
        String normalizedName = normalizeName(name);
        if (normalizedName == null) {
            return false;
        }
        String normalizedExcludedId = normalizeId(excludedId);
        for (QuickTemplate template : readAll()) {
            if (normalizedExcludedId != null && normalizedExcludedId.equals(template.id)) {
                continue;
            }
            String candidateName = normalizeName(template.name);
            if (normalizedName.equals(candidateName)) {
                return true;
            }
        }
        return false;
    }

    public boolean save(QuickTemplate template) {
        String normalizedId = template != null ? normalizeId(template.id) : null;
        if (template == null
                || normalizedId == null
                || template.name == null
                || template.name.trim().isEmpty()) {
            return false;
        }
        LinkedHashSet<String> ids = getTemplateIds();
        boolean existingTemplate = ids.contains(normalizedId);
        ids.add(normalizedId);
        String prefix = prefixFor(normalizedId);
        SharedPreferences.Editor editor = preferences.edit()
                .putStringSet(KEY_TEMPLATE_IDS, ids)
                .putString(prefix + "name", template.name.trim())
                .putLong(prefix + "updated_at", template.updatedAt)
                .putStringSet(prefix + "selected_packages",
                        sanitizeStringSet(template.selectedPackages));
        editor.putString(KEY_TEMPLATE_ORDER, orderAfterSave(existingTemplate, normalizedId));
        TemplateConfigPreferences.write(editor, prefix + "config.",
                TemplateCustomSemantics.customValue(template.configValue));
        return editor.commit();
    }

    public boolean reorder(List<String> orderedIds) {
        LinkedHashSet<String> currentIds = getTemplateIds();
        if (currentIds.isEmpty()) {
            return preferences.edit()
                    .remove(KEY_TEMPLATE_ORDER)
                    .commit();
        }
        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        if (orderedIds != null) {
            for (String id : orderedIds) {
                String normalizedId = normalizeId(id);
                if (normalizedId != null && currentIds.contains(normalizedId) && read(normalizedId) != null) {
                    sanitized.add(normalizedId);
                }
            }
        }
        for (String id : currentIds) {
            if (!sanitized.contains(id) && read(id) != null) {
                sanitized.add(id);
            }
        }
        return preferences.edit()
                .putStringSet(KEY_TEMPLATE_IDS, sanitized)
                .putString(KEY_TEMPLATE_ORDER, String.join("\n", sanitized))
                .commit();
    }

    public boolean setSelectedPackages(String id, Set<String> packageNames) {
        String normalizedId = normalizeId(id);
        if (normalizedId == null || !getTemplateIds().contains(normalizedId)) {
            return false;
        }
        return preferences.edit()
                .putStringSet(prefixFor(normalizedId) + "selected_packages",
                        sanitizeStringSet(packageNames))
                .commit();
    }

    public boolean delete(String id) {
        String normalizedId = normalizeId(id);
        if (normalizedId == null) {
            return false;
        }
        LinkedHashSet<String> ids = getTemplateIds();
        ids.remove(normalizedId);
        String order = orderAfterDelete(normalizedId, ids);
        String prefix = prefixFor(normalizedId);
        SharedPreferences.Editor editor = preferences.edit()
                .putStringSet(KEY_TEMPLATE_IDS, ids)
                .putString(KEY_TEMPLATE_ORDER, order)
                .remove(prefix + "name")
                .remove(prefix + "updated_at")
                .remove(prefix + "selected_packages");
        TemplateConfigPreferences.clear(editor, prefix + "config.");
        return editor.commit();
    }

    private LinkedHashSet<String> getTemplateIds() {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        Set<String> storedIds = getStringSet(KEY_TEMPLATE_IDS);
        if (storedIds == null) {
            return ids;
        }
        for (String id : storedIds) {
            String normalized = normalizeId(id);
            if (normalized != null) {
                ids.add(normalized);
            }
        }
        return ids;
    }

    private List<String> getTemplateOrderIds() {
        String rawOrder = getString(KEY_TEMPLATE_ORDER, null);
        if (rawOrder == null || rawOrder.isBlank()) {
            return Collections.emptyList();
        }
        ArrayList<String> ids = new ArrayList<>();
        for (String part : rawOrder.split("\\n")) {
            String normalized = normalizeId(part);
            if (normalized != null && !ids.contains(normalized)) {
                ids.add(normalized);
            }
        }
        return ids;
    }

    private LinkedHashSet<String> getSelectedPackages(String id) {
        return sanitizeStringSet(getStringSet(prefixFor(id) + "selected_packages"));
    }

    private Set<String> getStringSet(String key) {
        try {
            return preferences.getStringSet(key, Collections.emptySet());
        } catch (ClassCastException ignored) {
            return Collections.emptySet();
        }
    }

    private String getString(String key, String defaultValue) {
        try {
            return preferences.getString(key, defaultValue);
        } catch (ClassCastException ignored) {
            return defaultValue;
        }
    }

    private long getLong(String key, long defaultValue) {
        try {
            return preferences.getLong(key, defaultValue);
        } catch (ClassCastException ignored) {
            return defaultValue;
        }
    }

    private static String prefixFor(String id) {
        return "template." + id + ".";
    }

    private static String normalizeId(String id) {
        if (id == null) {
            return null;
        }
        String trimmed = id.trim();
        return trimmed.isEmpty() || trimmed.contains(".") ? null : trimmed;
    }

    private static String normalizeName(String name) {
        if (name == null) {
            return null;
        }
        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private static LinkedHashSet<String> sanitizeStringSet(Set<String> values) {
        LinkedHashSet<String> sanitized = new LinkedHashSet<>();
        if (values == null) {
            return sanitized;
        }
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                sanitized.add(trimmed);
            }
        }
        return sanitized;
    }

    private static String appendToOrder(String existingOrder, String id) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        if (existingOrder != null && !existingOrder.isBlank()) {
            for (String part : existingOrder.split("\\n")) {
                String normalized = normalizeId(part);
                if (normalized != null) {
                    ordered.add(normalized);
                }
            }
        }
        ordered.add(id);
        return String.join("\n", ordered);
    }

    private String orderAfterDelete(String deletedId, LinkedHashSet<String> remainingIds) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (String id : getTemplateOrderIds()) {
            if (!id.equals(deletedId) && remainingIds.contains(id)) {
                ordered.add(id);
            }
        }
        for (String id : remainingIds) {
            if (!id.equals(deletedId)) {
                ordered.add(id);
            }
        }
        return String.join("\n", ordered);
    }

    private String orderAfterSave(boolean existingTemplate, String savedId) {
        String existingOrder = getString(KEY_TEMPLATE_ORDER, null);
        if (existingOrder != null && !existingOrder.isBlank()) {
            return appendToOrder(existingOrder, savedId);
        }
        ArrayList<QuickTemplate> existingTemplates = new ArrayList<>();
        for (String id : getTemplateIds()) {
            QuickTemplate template = read(id);
            if (template != null) {
                existingTemplates.add(template);
            }
        }
        existingTemplates.sort(defaultComparator());
        ArrayList<String> orderedIds = new ArrayList<>(existingTemplates.size() + 1);
        for (QuickTemplate template : existingTemplates) {
            if (!orderedIds.contains(template.id)) {
                orderedIds.add(template.id);
            }
        }
        if (!existingTemplate) {
            orderedIds.remove(savedId);
            orderedIds.add(savedId);
        } else if (!orderedIds.contains(savedId)) {
            orderedIds.add(savedId);
        }
        return String.join("\n", orderedIds);
    }

    private static Comparator<QuickTemplate> defaultComparator() {
        return Comparator
                .comparingLong((QuickTemplate template) -> template.updatedAt).reversed()
                .thenComparing(template -> template.name)
                .thenComparing(template -> template.id);
    }

    public static final class QuickTemplate implements QuickTemplateApplyCoordinator.Template<TemplateConfigValue> {
        public final String id;
        public final String name;
        public final long updatedAt;
        public final Set<String> selectedPackages;
        public final TemplateConfigValue configValue;

        public QuickTemplate(
                String id,
                String name,
                long updatedAt,
                Set<String> selectedPackages,
                TemplateConfigValue configValue) {
            this.id = id;
            this.name = name;
            this.updatedAt = updatedAt;
            this.selectedPackages = Collections.unmodifiableSet(
                    sanitizeStringSet(selectedPackages));
            this.configValue = configValue != null ? configValue : TemplateConfigValue.EMPTY;
        }

        @Override
        public Set<String> selectedPackages() {
            return selectedPackages;
        }

        @Override
        public TemplateConfigValue configValue() {
            return configValue;
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof QuickTemplate other)) {
                return false;
            }
            return updatedAt == other.updatedAt
                    && id.equals(other.id)
                    && name.equals(other.name)
                    && selectedPackages.equals(other.selectedPackages)
                    && configValue.equals(other.configValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, name, updatedAt, selectedPackages, configValue);
        }
    }
}
