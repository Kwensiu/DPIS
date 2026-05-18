package com.dpis.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class FontHookDomainRegistry {
    static final String ID_RESOURCES_FONT = "resources_font";
    static final String ID_ACTIVITY_THREAD_FONT = "activity_thread_font";
    static final String ID_TEXTVIEW_SP_REWRITE = "textview_sp_rewrite";
    static final String ID_TEXTVIEW_ABSOLUTE_REWRITE = "textview_absolute_rewrite";
    static final String ID_TEXTVIEW_CURRENT_PX_FALLBACK = "textview_current_px_fallback";
    static final String ID_PAINT_TEXT_SIZE_FALLBACK = "paint_text_size_fallback";
    static final String ID_WEBVIEW_TEXT_ZOOM = "webview_text_zoom";
    static final String ID_FLUTTER_SETTINGS = "flutter_settings";
    static final String ID_HYPEROS_NATIVE_FLUTTER = "hyperos_native_flutter";

    private static final List<String> ORDERED_IDS = List.of(
            ID_RESOURCES_FONT,
            ID_ACTIVITY_THREAD_FONT,
            ID_TEXTVIEW_SP_REWRITE,
            ID_TEXTVIEW_ABSOLUTE_REWRITE,
            ID_TEXTVIEW_CURRENT_PX_FALLBACK,
            ID_PAINT_TEXT_SIZE_FALLBACK,
            ID_WEBVIEW_TEXT_ZOOM,
            ID_FLUTTER_SETTINGS,
            ID_HYPEROS_NATIVE_FLUTTER);

    private FontHookDomainRegistry() {
    }

    static Set<String> knownDomainIds() {
        return new LinkedHashSet<>(ORDERED_IDS);
    }

    static boolean isKnown(String domainId) {
        return domainId != null && ORDERED_IDS.contains(domainId);
    }

    static Set<String> orderedKnownSubset(Set<String> domains) {
        if (domains == null || domains.isEmpty()) {
            return new LinkedHashSet<>();
        }
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        for (String id : ORDERED_IDS) {
            if (domains.contains(id)) {
                ordered.add(id);
            }
        }
        return ordered;
    }

    static List<String> orderedIdsList() {
        return Collections.unmodifiableList(new ArrayList<>(ORDERED_IDS));
    }
}
