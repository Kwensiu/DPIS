package com.dpis.module;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class OpenSourceLicenseActivity extends LocalizedActivity {
    private static final class LicenseItem {
        final String name;
        final String summary;
        final String detail;
        final String website;

        LicenseItem(String name, String summary, String detail, String website) {
            this.name = name;
            this.summary = summary;
            this.detail = detail;
            this.website = website;
        }
    }

    static final class ResolvedLicense {
        final String id;
        final String name;
        final String url;
        final String content;

        ResolvedLicense(String id, String name, String url, String content) {
            this.id = id;
            this.name = name;
            this.url = url;
            this.content = content;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_source_license);
        applyInsets();

        ImageButton backButton = findViewById(R.id.open_source_license_back_button);
        backButton.setOnClickListener(v -> finish());

        List<LicenseItem> licenseItems = loadLicenseItems();
        ListView listView = findViewById(R.id.open_source_license_list);
        listView.setSelector(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        listView.setAdapter(new LicenseAdapter(LayoutInflater.from(this), licenseItems));
        listView.setOnItemClickListener((parent, view, position, id) -> {
            view.setPressed(false);
            listView.clearChoices();
            LicenseItem item = licenseItems.get(position);
            showLicenseDetailDialog(item);
        });
    }

    private void applyInsets() {
        View content = findViewById(R.id.open_source_license_content);
        final int baseTopPadding = content.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(view.getPaddingLeft(), baseTopPadding + statusBars.top,
                    view.getPaddingRight(), view.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
    }

    private List<LicenseItem> loadLicenseItems() {
        try (InputStream inputStream = getResources().openRawResource(R.raw.aboutlibraries)) {
            String rawJson = readText(inputStream);
            JSONObject root = new JSONObject(rawJson);
            JSONArray libraries = root.optJSONArray("libraries");
            JSONObject licenseCatalog = root.optJSONObject("licenses");
            if (libraries == null || libraries.length() == 0) {
                List<LicenseItem> items = new ArrayList<>();
                items.add(createProjectLicenseItem());
                items.add(emptyItem(getString(R.string.open_source_license_empty)));
                return items;
            }

            List<LicenseItem> items = new ArrayList<>();
            items.add(createProjectLicenseItem());
            for (int i = 0; i < libraries.length(); i++) {
                JSONObject library = libraries.optJSONObject(i);
                if (library == null) {
                    continue;
                }
                String name = firstNonEmpty(
                        library.optString("name"),
                        library.optString("artifactId"),
                        library.optString("uniqueId"),
                        getString(R.string.open_source_license_name_unknown)
                );
                String version = firstNonEmpty(
                        library.optString("artifactVersion"),
                        library.optString("version"),
                        ""
                );
                List<ResolvedLicense> resolvedLicenses =
                        resolveLicenses(library.optJSONArray("licenses"), licenseCatalog);
                String licenseNames = collectLicenseNames(resolvedLicenses);
                if (licenseNames.isEmpty()) {
                    licenseNames = getString(R.string.open_source_license_item_fallback);
                }
                String website = firstNonEmpty(
                        library.optString("website"),
                        collectScmUrl(library),
                        collectOrganizationUrl(library),
                        collectFirstLicenseUrl(resolvedLicenses)
                );

                StringBuilder detailBuilder = new StringBuilder();
                if (!version.isEmpty()) {
                    detailBuilder.append(getString(R.string.open_source_license_version_label, version));
                }
                if (!website.isEmpty()) {
                    if (detailBuilder.length() > 0) {
                        detailBuilder.append('\n');
                    }
                    detailBuilder.append(getString(R.string.open_source_license_website_label, website));
                }
                String licenseDetail = buildLicenseDetail(resolvedLicenses);
                if (detailBuilder.length() > 0) {
                    detailBuilder.append('\n');
                }
                detailBuilder.append(licenseDetail.isEmpty() ? licenseNames : licenseDetail);

                items.add(new LicenseItem(name, licenseNames, detailBuilder.toString(), website));
            }

            items.subList(1, items.size()).sort((left, right) -> left.name.compareToIgnoreCase(right.name));
            return items;
        } catch (Resources.NotFoundException e) {
            List<LicenseItem> items = new ArrayList<>();
            items.add(createProjectLicenseItem());
            items.add(emptyItem(getString(R.string.open_source_license_empty)));
            return items;
        } catch (Throwable t) {
            List<LicenseItem> items = new ArrayList<>();
            items.add(createProjectLicenseItem());
            items.add(emptyItem(getString(R.string.open_source_license_load_failed)));
            return items;
        }
    }

    private LicenseItem createProjectLicenseItem() {
        String sourceUrl = getString(R.string.about_source_url);
        StringBuilder detailBuilder = new StringBuilder(
                getString(R.string.open_source_license_project_detail, sourceUrl));
        try (InputStream inputStream = getResources().openRawResource(R.raw.gpl_3_0)) {
            detailBuilder.append("\n\n").append(readText(inputStream));
        } catch (Throwable ignored) {
        }
        return new LicenseItem(
                getString(R.string.app_name),
                getString(R.string.open_source_license_project_summary),
                detailBuilder.toString(),
                sourceUrl
        );
    }

    private LicenseItem emptyItem(String reason) {
        return new LicenseItem(
                getString(R.string.open_source_license),
                reason,
                reason,
                ""
        );
    }

    private void showLicenseDetailDialog(LicenseItem item) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(item.name)
                .setMessage(item.detail)
                .setPositiveButton(R.string.dialog_close_button, null);
        if (!item.website.isEmpty()) {
            builder.setNeutralButton(R.string.about_link_source_title,
                    (dialog, which) -> openUrl(item.website));
        }
        builder.show();
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            Toast.makeText(this, R.string.about_link_open_failed, Toast.LENGTH_SHORT).show();
        }
    }

    static List<ResolvedLicense> resolveLicenses(JSONArray licenses, JSONObject licenseCatalog) {
        if (licenses == null || licenses.length() == 0) {
            return Collections.emptyList();
        }

        List<ResolvedLicense> resolvedLicenses = new ArrayList<>();
        Set<String> seenIds = new LinkedHashSet<>();
        for (int i = 0; i < licenses.length(); i++) {
            ResolvedLicense resolved = resolveLicenseEntry(licenses.opt(i), licenseCatalog);
            if (resolved == null) {
                continue;
            }
            String dedupeKey = firstNonEmpty(resolved.id, resolved.name, resolved.url, String.valueOf(i));
            if (!seenIds.add(dedupeKey)) {
                continue;
            }
            resolvedLicenses.add(resolved);
        }
        return resolvedLicenses;
    }

    private static ResolvedLicense resolveLicenseEntry(Object entry, JSONObject licenseCatalog) {
        if (entry instanceof String) {
            return mergeLicenseInfo((String) entry, null, licenseCatalog);
        }
        if (entry instanceof JSONObject) {
            JSONObject inline = (JSONObject) entry;
            String key = firstNonEmpty(
                    inline.optString("internalHash"),
                    inline.optString("spdxId"),
                    inline.optString("hash"),
                    ""
            );
            return mergeLicenseInfo(key, inline, licenseCatalog);
        }
        return null;
    }

    private static ResolvedLicense mergeLicenseInfo(String key, JSONObject inline, JSONObject licenseCatalog) {
        String normalizedKey = firstNonEmpty(key, "");
        JSONObject catalog = normalizedKey.isEmpty() || licenseCatalog == null
                ? null
                : licenseCatalog.optJSONObject(normalizedKey);
        String id = firstNonEmpty(
                inline == null ? "" : inline.optString("internalHash"),
                inline == null ? "" : inline.optString("spdxId"),
                inline == null ? "" : inline.optString("hash"),
                catalog == null ? "" : catalog.optString("internalHash"),
                catalog == null ? "" : catalog.optString("spdxId"),
                normalizedKey
        );
        String name = firstNonEmpty(
                inline == null ? "" : inline.optString("name"),
                inline == null ? "" : inline.optString("spdxId"),
                catalog == null ? "" : catalog.optString("name"),
                catalog == null ? "" : catalog.optString("spdxId"),
                id
        );
        String url = firstNonEmpty(
                inline == null ? "" : inline.optString("url"),
                catalog == null ? "" : catalog.optString("url"),
                ""
        );
        String content = firstNonEmpty(
                inline == null ? "" : inline.optString("content"),
                catalog == null ? "" : catalog.optString("content"),
                ""
        );

        if (name.isEmpty() && url.isEmpty() && content.isEmpty()) {
            return null;
        }
        return new ResolvedLicense(id, name, url, content);
    }

    static String collectLicenseNames(List<ResolvedLicense> resolvedLicenses) {
        if (resolvedLicenses == null || resolvedLicenses.isEmpty()) {
            return "";
        }
        List<String> names = new ArrayList<>();
        for (ResolvedLicense license : resolvedLicenses) {
            if (!license.name.isEmpty()) {
                names.add(license.name);
            }
        }
        if (names.isEmpty()) {
            return "";
        }
        return String.join(", ", names);
    }

    static String collectFirstLicenseUrl(List<ResolvedLicense> resolvedLicenses) {
        if (resolvedLicenses == null || resolvedLicenses.isEmpty()) {
            return "";
        }
        for (ResolvedLicense license : resolvedLicenses) {
            if (!license.url.isEmpty()) {
                return license.url;
            }
        }
        return "";
    }

    static String buildLicenseDetail(List<ResolvedLicense> resolvedLicenses) {
        if (resolvedLicenses == null || resolvedLicenses.isEmpty()) {
            return "";
        }
        StringBuilder detailBuilder = new StringBuilder();
        for (ResolvedLicense license : resolvedLicenses) {
            if (detailBuilder.length() > 0) {
                detailBuilder.append("\n\n");
            }
            detailBuilder.append(license.name);
            if (!license.url.isEmpty()) {
                detailBuilder.append('\n').append(license.url);
            }
            if (!license.content.isEmpty()) {
                detailBuilder.append("\n\n").append(license.content);
            }
        }
        return detailBuilder.toString();
    }

    private static String collectScmUrl(JSONObject library) {
        Object scm = library.opt("scm");
        if (scm instanceof JSONObject) {
            JSONObject scmObject = (JSONObject) scm;
            return firstNonEmpty(
                    scmObject.optString("url"),
                    scmObject.optString("developerConnection"),
                    scmObject.optString("connection"),
                    ""
            );
        }
        if (scm instanceof String) {
            return firstNonEmpty((String) scm, "");
        }
        return "";
    }

    private static String collectOrganizationUrl(JSONObject library) {
        String direct = firstNonEmpty(library.optString("organizationUrl"), "");
        if (!direct.isEmpty()) {
            return direct;
        }
        Object organization = library.opt("organization");
        if (organization instanceof JSONObject) {
            JSONObject organizationObject = (JSONObject) organization;
            return firstNonEmpty(
                    organizationObject.optString("url"),
                    organizationObject.optString("organisationUrl"),
                    ""
            );
        }
        return "";
    }

    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null) {
                String trimmed = value.trim();
                if (!trimmed.isEmpty()) {
                    return trimmed;
                }
            }
        }
        return "";
    }

    private static String readText(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, count);
        }
        return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
    }

    private static final class LicenseAdapter extends BaseAdapter {
        private final LayoutInflater inflater;
        private final List<LicenseItem> items;

        LicenseAdapter(LayoutInflater inflater, List<LicenseItem> items) {
            this.inflater = inflater;
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View root = convertView;
            if (root == null) {
                root = inflater.inflate(R.layout.item_open_source_license, parent, false);
            }
            LicenseItem item = items.get(position);
            TextView nameView = root.findViewById(R.id.license_item_name);
            TextView summaryView = root.findViewById(R.id.license_item_summary);
            nameView.setText(item.name);
            String summary = item.summary.isEmpty()
                    ? root.getResources().getString(R.string.open_source_license_item_fallback)
                    : item.summary;
            summaryView.setText(summary);
            return root;
        }
    }
}
