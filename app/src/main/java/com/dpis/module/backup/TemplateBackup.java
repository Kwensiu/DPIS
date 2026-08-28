package com.dpis.module.backup;

import com.dpis.module.templates.TemplateConfigValue;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/** Portable representation of one quick template, independent of preference keys. */
public final class TemplateBackup {
    public final String id;
    public final String name;
    public final long updatedAt;
    public final Set<String> selectedPackages;
    public final TemplateConfigValue configValue;

    public TemplateBackup(String id, String name, long updatedAt,
                          Set<String> selectedPackages, TemplateConfigValue configValue) {
        this.id = id;
        this.name = name;
        this.updatedAt = updatedAt;
        this.selectedPackages = selectedPackages == null
                ? Collections.emptySet() : Collections.unmodifiableSet(new LinkedHashSet<>(selectedPackages));
        this.configValue = configValue == null ? TemplateConfigValue.EMPTY : configValue;
    }
}
