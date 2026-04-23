package com.dpis.module;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.content.pm.SigningInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textview.MaterialTextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AboutActivity extends Activity {
    private static final int UPDATE_CONNECT_TIMEOUT_MS = 10_000;
    private static final int UPDATE_READ_TIMEOUT_MS = 10_000;
    private static final int DOWNLOAD_BUFFER_SIZE = 16 * 1024;
    private static final long DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS = 180L;
    private static final Pattern LEADING_NUMBER_PATTERN = Pattern.compile("^(\\d+)");

    private final ExecutorService updateExecutor = Executors.newSingleThreadExecutor();
    private volatile boolean updateCheckInProgress = false;
    private volatile boolean updateDownloadInProgress = false;
    private volatile boolean updateDownloadCancelRequested = false;
    private volatile Future<?> activeDownloadFuture;
    private volatile HttpURLConnection activeDownloadConnection;

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
        cancelActiveUpdateDownload();
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
        if (updateDownloadInProgress) {
            showToast(R.string.about_update_download_in_progress);
            return;
        }
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
        showUpdateDialog(manifest);
    }

    private void showUpdateDialog(UpdateManifest manifest) {
        String releasePageUrl = manifest.releasePage.isEmpty()
                ? getString(R.string.about_releases_url)
                : manifest.releasePage;
        String downloadUrl = manifest.apkUrl;
        showCenteredUpdateDialog(manifest, downloadUrl, releasePageUrl);
    }

    private void showCenteredUpdateDialog(UpdateManifest manifest,
                                          String downloadUrl,
                                          String releasePageUrl) {
        UpdateAvailableDialog.DialogHandle dialogHandle = UpdateAvailableDialog.create(
                this,
                getString(R.string.about_update_available_title),
                getString(
                        R.string.about_update_available_message,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                        manifest.versionName,
                        manifest.versionCode));

        AlertDialog dialog = dialogHandle.dialog;
        MaterialButton primaryButton = dialogHandle.primaryButton;
        MaterialButton cancelButton = dialogHandle.cancelButton;
        LinearProgressIndicator progressView = dialogHandle.progressView;
        MaterialTextView progressTextView = dialogHandle.progressTextView;

        showDialogIdleState(primaryButton, progressView, progressTextView);
        bindDialogCancelButton(dialog, cancelButton);

        boolean hasDirectDownload = downloadUrl != null && !downloadUrl.trim().isEmpty();
        if (!hasDirectDownload) {
            primaryButton.setText(R.string.about_update_action_view_release);
            primaryButton.setOnClickListener(v -> openUrl(releasePageUrl));
            dialog.show();
            return;
        }

        primaryButton.setOnClickListener(v -> {
            if (updateDownloadInProgress) {
                cancelActiveUpdateDownload();
                return;
            }
            startUpdateDownload(
                    manifest.versionName,
                    downloadUrl,
                    dialog,
                    primaryButton,
                    progressView,
                    progressTextView);
        });

        dialog.setOnDismissListener(unused -> cancelActiveUpdateDownload());
        dialog.show();
    }

    private void bindDialogCancelButton(AlertDialog dialog, MaterialButton cancelButton) {
        cancelButton.setOnClickListener(v -> {
            if (updateDownloadInProgress) {
                cancelActiveUpdateDownload();
            }
            dialog.dismiss();
        });
    }

    private void startUpdateDownload(String targetVersionName,
                                     String downloadUrl,
                                     AlertDialog dialog,
                                     MaterialButton primaryButton,
                                     LinearProgressIndicator progressView,
                                     MaterialTextView progressTextView) {
        if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
            showToast(R.string.about_update_download_failed);
            return;
        }

        Uri downloadUri = Uri.parse(downloadUrl);
        String scheme = downloadUri.getScheme();
        if (scheme == null || !"https".equalsIgnoreCase(scheme)) {
            showToast(R.string.about_update_download_https_required);
            return;
        }

        final File targetFile;
        try {
            UpdatePackageInstaller.clearUpdateCache(this);
            targetFile = UpdatePackageInstaller.prepareTargetFile(this, targetVersionName);
        } catch (RuntimeException ignored) {
            showToast(R.string.about_update_download_failed);
            return;
        }

        updateDownloadInProgress = true;
        updateDownloadCancelRequested = false;
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        showDialogDownloadingState(primaryButton, progressView, progressTextView);

        activeDownloadFuture = updateExecutor.submit(() -> executeApkDownload(
                downloadUri,
                targetFile,
                dialog,
                primaryButton,
                progressView,
                progressTextView));
    }

    private void executeApkDownload(Uri downloadUri,
                                    File targetFile,
                                    AlertDialog dialog,
                                    MaterialButton primaryButton,
                                    LinearProgressIndicator progressView,
                                    MaterialTextView progressTextView) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(downloadUri.toString()).openConnection();
            activeDownloadConnection = connection;
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(UPDATE_CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(UPDATE_READ_TIMEOUT_MS);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/octet-stream,*/*");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected HTTP response code: " + responseCode);
            }

            long totalBytes = connection.getContentLengthLong();
            runOnUiThread(() -> prepareProgressView(progressView, progressTextView, totalBytes));

            try (InputStream inputStream = connection.getInputStream();
                 FileOutputStream outputStream = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
                long downloadedBytes = 0L;
                long lastUiUpdateAt = 0L;
                int lastProgress = -1;

                while (true) {
                    if (updateDownloadCancelRequested || Thread.currentThread().isInterrupted()) {
                        throw new DownloadCanceledException();
                    }
                    int read = inputStream.read(buffer);
                    if (read < 0) {
                        break;
                    }
                    outputStream.write(buffer, 0, read);
                    downloadedBytes += read;

                    long now = System.currentTimeMillis();
                    if (now - lastUiUpdateAt < DOWNLOAD_PROGRESS_UPDATE_INTERVAL_MS
                            && totalBytes > 0L) {
                        continue;
                    }

                    lastUiUpdateAt = now;
                    if (totalBytes > 0L) {
                        int progress = (int) Math.min(100L, (downloadedBytes * 100L) / totalBytes);
                        if (progress != lastProgress) {
                            lastProgress = progress;
                            long finalDownloadedBytes = downloadedBytes;
                            runOnUiThread(() -> updateProgressView(
                                    progressView,
                                    progressTextView,
                                    progress,
                                    finalDownloadedBytes,
                                    totalBytes));
                        }
                    } else {
                        long finalDownloadedBytes = downloadedBytes;
                        runOnUiThread(() -> updateProgressViewWithoutTotal(
                                progressView,
                                progressTextView,
                                finalDownloadedBytes));
                    }
                }
                outputStream.flush();
            }

            if (updateDownloadCancelRequested) {
                throw new DownloadCanceledException();
            }

            verifyDownloadedApk(targetFile);
            UpdatePackageInstaller.persistDownloadedFile(this, targetFile);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (dialog.isShowing()) {
                    dialog.dismiss();
                }
                launchPackageInstaller(targetFile);
            });
        } catch (DownloadCanceledException ignored) {
            safeDeleteFile(targetFile);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showDialogIdleState(primaryButton, progressView, progressTextView);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                showToast(R.string.about_update_download_canceled);
            });
        } catch (UntrustedUpdateException ignored) {
            safeDeleteFile(targetFile);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showDialogIdleState(primaryButton, progressView, progressTextView);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                showToast(R.string.about_update_download_untrusted);
            });
        } catch (Exception ignored) {
            safeDeleteFile(targetFile);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                showDialogIdleState(primaryButton, progressView, progressTextView);
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(true);
                showToast(R.string.about_update_download_failed);
            });
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            activeDownloadConnection = null;
            activeDownloadFuture = null;
            updateDownloadInProgress = false;
            updateDownloadCancelRequested = false;
        }
    }

    private void cancelActiveUpdateDownload() {
        if (!updateDownloadInProgress) {
            return;
        }
        updateDownloadCancelRequested = true;
        HttpURLConnection connection = activeDownloadConnection;
        if (connection != null) {
            connection.disconnect();
        }
        Future<?> future = activeDownloadFuture;
        if (future != null) {
            future.cancel(true);
        }
    }

    private void showDialogIdleState(MaterialButton primaryButton,
                                     LinearProgressIndicator progressView,
                                     MaterialTextView progressTextView) {
        primaryButton.setText(R.string.about_update_action_download);
        progressView.setVisibility(View.GONE);
        progressTextView.setVisibility(View.GONE);
    }

    private void showDialogDownloadingState(MaterialButton primaryButton,
                                            LinearProgressIndicator progressView,
                                            MaterialTextView progressTextView) {
        primaryButton.setText(R.string.about_update_action_cancel_download);
        progressView.setVisibility(View.VISIBLE);
        progressTextView.setVisibility(View.VISIBLE);
        progressView.setIndeterminate(true);
        progressTextView.setText(R.string.about_update_download_progress_preparing);
    }

    private void prepareProgressView(LinearProgressIndicator progressView,
                                     MaterialTextView progressTextView,
                                     long totalBytes) {
        if (totalBytes > 0L) {
            progressView.setIndeterminate(false);
            progressView.setProgress(0);
            updateProgressView(progressView, progressTextView, 0, 0L, totalBytes);
            return;
        }
        progressView.setIndeterminate(true);
        updateProgressViewWithoutTotal(progressView, progressTextView, 0L);
    }

    private void updateProgressView(LinearProgressIndicator progressView,
                                    MaterialTextView progressTextView,
                                    int progress,
                                    long downloadedBytes,
                                    long totalBytes) {
        if (progressView.isIndeterminate()) {
            progressView.setIndeterminate(false);
        }
        progressView.setProgress(progress);
        progressTextView.setText(getString(
                R.string.about_update_download_progress_with_percent,
                progress,
                formatBytes(downloadedBytes),
                formatBytes(totalBytes)));
    }

    private void updateProgressViewWithoutTotal(LinearProgressIndicator progressView,
                                                MaterialTextView progressTextView,
                                                long downloadedBytes) {
        progressView.setIndeterminate(true);
        progressTextView.setText(getString(
                R.string.about_update_download_progress_without_total,
                formatBytes(downloadedBytes)));
    }

    private void verifyDownloadedApk(File apkFile) throws UntrustedUpdateException {
        PackageManager packageManager = getPackageManager();
        PackageInfo downloadedPackage = readArchivePackageInfo(packageManager, apkFile);
        if (downloadedPackage == null
                || downloadedPackage.packageName == null
                || !getPackageName().equals(downloadedPackage.packageName)) {
            throw new UntrustedUpdateException();
        }

        PackageInfo installedPackage;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                installedPackage = packageManager.getPackageInfo(
                        getPackageName(),
                        PackageManager.GET_SIGNING_CERTIFICATES);
            } else {
                installedPackage = packageManager.getPackageInfo(
                        getPackageName(),
                        PackageManager.GET_SIGNATURES);
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new UntrustedUpdateException();
        }

        Set<String> downloadedSignatures = extractSigningFingerprints(downloadedPackage);
        Set<String> installedSignatures = extractSigningFingerprints(installedPackage);
        if (downloadedSignatures.isEmpty()
                || installedSignatures.isEmpty()
                || !downloadedSignatures.equals(installedSignatures)) {
            throw new UntrustedUpdateException();
        }
    }

    private static PackageInfo readArchivePackageInfo(PackageManager packageManager, File apkFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return packageManager.getPackageArchiveInfo(
                    apkFile.getAbsolutePath(),
                    PackageManager.GET_SIGNING_CERTIFICATES);
        }
        return packageManager.getPackageArchiveInfo(
                apkFile.getAbsolutePath(),
                PackageManager.GET_SIGNATURES);
    }

    private static Set<String> extractSigningFingerprints(PackageInfo packageInfo) {
        Signature[] signatures;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            SigningInfo signingInfo = packageInfo.signingInfo;
            if (signingInfo == null) {
                return new HashSet<>();
            }
            signatures = signingInfo.hasMultipleSigners()
                    ? signingInfo.getApkContentsSigners()
                    : signingInfo.getSigningCertificateHistory();
        } else {
            signatures = packageInfo.signatures;
        }
        return signaturesToFingerprints(signatures);
    }

    private static Set<String> signaturesToFingerprints(Signature[] signatures) {
        Set<String> fingerprints = new HashSet<>();
        if (signatures == null) {
            return fingerprints;
        }
        for (Signature signature : signatures) {
            if (signature == null) {
                continue;
            }
            fingerprints.add(toSha256Hex(signature.toByteArray()));
        }
        return fingerprints;
    }

    private static String toSha256Hex(byte[] value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(value));
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }

    private static String toHex(byte[] value) {
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte item : value) {
            builder.append(Character.forDigit((item >> 4) & 0xF, 16));
            builder.append(Character.forDigit(item & 0xF, 16));
        }
        return builder.toString();
    }

    private void launchPackageInstaller(File apkFile) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !getPackageManager().canRequestPackageInstalls()) {
            Intent settingsIntent = new Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + getPackageName()));
            startActivity(settingsIntent);
            showToast(R.string.about_update_install_permission_required);
            return;
        }
        try {
            Uri contentUri = UpdatePackageInstaller.getInstallUri(this, apkFile);
            Intent installIntent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(contentUri, UpdatePackageInstaller.APK_MIME_TYPE)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(installIntent);
        } catch (ActivityNotFoundException | IllegalArgumentException ignored) {
            showToast(R.string.about_update_install_failed);
        }
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

    private static String formatBytes(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        double value = bytes;
        String[] units = {"KB", "MB", "GB", "TB"};
        int unitIndex = -1;
        do {
            value /= 1024.0;
            unitIndex++;
        } while (value >= 1024.0 && unitIndex < units.length - 1);
        return String.format(Locale.getDefault(), "%.1f %s", value, units[unitIndex]);
    }

    private static void safeDeleteFile(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        //noinspection ResultOfMethodCallIgnored
        file.delete();
    }

    private static final class DownloadCanceledException extends IOException {
    }

    private static final class UntrustedUpdateException extends IOException {
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
