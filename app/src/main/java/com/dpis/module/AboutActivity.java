package com.dpis.module;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textview.MaterialTextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AboutActivity extends Activity {
    private static final int UPDATE_CONNECT_TIMEOUT_MS = 10_000;
    private static final int UPDATE_READ_TIMEOUT_MS = 10_000;
    private static final Pattern LEADING_NUMBER_PATTERN = Pattern.compile("^(\\d+)");

    private final ExecutorService updateExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean updateCheckInProgress = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        applyInsets();

        ImageButton backButton = findViewById(R.id.about_back_button);
        backButton.setOnClickListener(v -> finish());

        MaterialTextView versionView = findViewById(R.id.about_version);
        versionView.setText(getString(R.string.about_version_format,
                BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        bindEntryRow(
                R.id.row_about_source,
                R.drawable.ic_info_outline_24,
                R.string.about_link_source_title,
                R.string.about_link_source_desc,
                getString(R.string.about_source_url));
        bindUpdateRow();
        bindEntryRow(
                R.id.row_about_feedback,
                R.drawable.ic_settings_24,
                R.string.about_link_feedback_title,
                R.string.about_link_feedback_desc,
                getString(R.string.about_issues_url));
        bindIntentEntryRow(
                R.id.row_about_open_source_license,
                R.drawable.ic_info_outline_24,
                R.string.open_source_license,
                R.string.open_source_license_settings_description,
                new Intent(this, OpenSourceLicenseActivity.class));
    }

    @Override
    protected void onDestroy() {
        updateExecutor.shutdownNow();
        super.onDestroy();
    }

    private void applyInsets() {
        View content = findViewById(R.id.about_content);
        final int baseTopPadding = content.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(content, (view, insets) -> {
            Insets statusBars = insets.getInsets(WindowInsetsCompat.Type.statusBars());
            view.setPadding(view.getPaddingLeft(), baseTopPadding + statusBars.top,
                    view.getPaddingRight(), view.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(content);
    }

    private void bindEntryRow(int rowId,
                              int iconRes,
                              int titleRes,
                              int subtitleRes,
                              String url) {
        View row = findViewById(rowId);
        ImageView iconView = row.findViewById(R.id.setting_icon);
        MaterialTextView titleView = row.findViewById(R.id.setting_title);
        MaterialTextView subtitleView = row.findViewById(R.id.setting_subtitle);
        iconView.setImageResource(iconRes);
        titleView.setText(titleRes);
        subtitleView.setText(subtitleRes);
        row.setOnClickListener(v -> openUrl(url));
    }

    private void bindIntentEntryRow(int rowId,
                                    int iconRes,
                                    int titleRes,
                                    int subtitleRes,
                                    Intent intent) {
        View row = findViewById(rowId);
        ImageView iconView = row.findViewById(R.id.setting_icon);
        MaterialTextView titleView = row.findViewById(R.id.setting_title);
        MaterialTextView subtitleView = row.findViewById(R.id.setting_subtitle);
        iconView.setImageResource(iconRes);
        titleView.setText(titleRes);
        subtitleView.setText(subtitleRes);
        row.setOnClickListener(v -> startActivity(intent));
    }

    private void bindUpdateRow() {
        View row = findViewById(R.id.row_about_update);
        ImageView iconView = row.findViewById(R.id.setting_icon);
        MaterialTextView titleView = row.findViewById(R.id.setting_title);
        MaterialTextView subtitleView = row.findViewById(R.id.setting_subtitle);
        iconView.setImageResource(R.drawable.ic_refresh_24);
        titleView.setText(R.string.about_link_update_title);
        subtitleView.setText(R.string.about_link_update_desc);
        row.setOnClickListener(v -> checkForUpdates());
    }

    private void checkForUpdates() {
        if (updateCheckInProgress) {
            showToast(R.string.about_update_checking);
            return;
        }
        updateCheckInProgress = true;
        showToast(R.string.about_update_checking);

        final String manifestUrl = getString(R.string.about_update_manifest_url);
        updateExecutor.execute(() -> {
            try {
                UpdateManifest manifest = fetchUpdateManifest(manifestUrl);
                runOnUiThread(() -> onUpdateManifestLoaded(manifest));
            } catch (Exception ignored) {
                runOnUiThread(() -> showToast(R.string.about_update_check_failed));
            } finally {
                runOnUiThread(() -> updateCheckInProgress = false);
            }
        });
    }

    private void onUpdateManifestLoaded(UpdateManifest manifest) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        boolean hasUpdate = isRemoteVersionNewer(
                manifest.versionCode,
                manifest.versionName,
                BuildConfig.VERSION_CODE,
                BuildConfig.VERSION_NAME);

        if (!hasUpdate) {
            showToast(R.string.about_update_up_to_date);
            return;
        }

        String primaryDownloadUrl = manifest.apkUrl.isEmpty()
                ? getString(R.string.about_releases_url)
                : manifest.apkUrl;
        String releasePageUrl = manifest.releasePage.isEmpty()
                ? getString(R.string.about_releases_url)
                : manifest.releasePage;

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.about_update_available_title, manifest.versionName))
                .setMessage(getString(
                        R.string.about_update_available_message,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                        manifest.versionName,
                        manifest.versionCode))
                .setPositiveButton(R.string.about_update_action_download,
                        (dialog, which) -> openUrl(primaryDownloadUrl))
                .setNegativeButton(android.R.string.cancel, null);

        if (!releasePageUrl.isEmpty()) {
            builder.setNeutralButton(R.string.about_update_action_view_release,
                    (dialog, which) -> openUrl(releasePageUrl));
        }
        builder.show();
    }

    private static UpdateManifest fetchUpdateManifest(String manifestUrl)
            throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(manifestUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(UPDATE_CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(UPDATE_READ_TIMEOUT_MS);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/json");

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected HTTP response code: " + responseCode);
            }
            String body = readUtf8(connection.getInputStream());
            JSONObject object = new JSONObject(body);
            String versionName = object.optString("version", "").trim();
            int versionCode = object.optInt("versionCode", 0);
            String apkUrl = object.optString("apkUrl", "").trim();
            String releasePage = object.optString("releasePage", "").trim();

            if (versionName.isEmpty() || versionCode <= 0) {
                throw new IOException("Invalid update manifest payload");
            }
            return new UpdateManifest(versionName, versionCode, apkUrl, releasePage);
        } finally {
            connection.disconnect();
        }
    }

    private static String readUtf8(InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
        }
        return builder.toString();
    }

    private static boolean isRemoteVersionNewer(int remoteCode,
                                                String remoteName,
                                                int localCode,
                                                String localName) {
        if (remoteCode > localCode) {
            return true;
        }
        if (remoteCode < localCode) {
            return false;
        }
        return compareSemVer(remoteName, localName) > 0;
    }

    private static int compareSemVer(String left, String right) {
        int[] leftParts = parseSemVer(left);
        int[] rightParts = parseSemVer(right);
        if (leftParts == null || rightParts == null) {
            return 0;
        }
        for (int i = 0; i < leftParts.length; i++) {
            if (leftParts[i] == rightParts[i]) {
                continue;
            }
            return leftParts[i] > rightParts[i] ? 1 : -1;
        }
        return 0;
    }

    private static int[] parseSemVer(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1);
        }
        String[] segments = normalized.split("\\.");
        if (segments.length < 3) {
            return null;
        }

        int[] result = new int[3];
        for (int i = 0; i < 3; i++) {
            Matcher matcher = LEADING_NUMBER_PATTERN.matcher(segments[i]);
            if (!matcher.find()) {
                return null;
            }
            try {
                result[i] = Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return result;
    }

    private void openUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            showToast(R.string.about_link_open_failed);
            return;
        }
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (ActivityNotFoundException ignored) {
            showToast(R.string.about_link_open_failed);
        }
    }

    private void showToast(int messageResId) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        Toast.makeText(this, messageResId, Toast.LENGTH_SHORT).show();
    }

    private static final class UpdateManifest {
        final String versionName;
        final int versionCode;
        final String apkUrl;
        final String releasePage;

        UpdateManifest(String versionName, int versionCode, String apkUrl, String releasePage) {
            this.versionName = versionName;
            this.versionCode = versionCode;
            this.apkUrl = apkUrl;
            this.releasePage = releasePage;
        }
    }
}
