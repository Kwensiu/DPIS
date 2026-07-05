package com.dpis.module.updates;

import com.dpis.module.BuildConfig;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public final class GitHubReleaseNotesFetcher {
    private GitHubReleaseNotesFetcher() {
    }

    public static String fetchLatest(int connectTimeoutMs, int readTimeoutMs) throws IOException {
        return fetchJsonBody(
                BuildConfig.GITHUB_RELEASES_API_URL + "/latest",
                connectTimeoutMs,
                readTimeoutMs);
    }

    public static String fetchByVersionName(String versionName, int connectTimeoutMs, int readTimeoutMs)
            throws IOException {
        String normalized = normalizeVersionName(versionName);
        if (normalized.isEmpty()) {
            return fetchLatest(connectTimeoutMs, readTimeoutMs);
        }
        IOException primaryError = null;
        try {
            return fetchJsonBody(
                    BuildConfig.GITHUB_RELEASES_API_URL + "/tags/v" + normalized,
                    connectTimeoutMs,
                    readTimeoutMs);
        } catch (IOException exception) {
            primaryError = exception;
        }
        try {
            return fetchJsonBody(
                    BuildConfig.GITHUB_RELEASES_API_URL + "/tags/" + normalized,
                    connectTimeoutMs,
                    readTimeoutMs);
        } catch (IOException fallbackError) {
            if (primaryError != null) {
                fallbackError.addSuppressed(primaryError);
            }
            throw fallbackError;
        }
    }

    private static String fetchJsonBody(String url, int connectTimeoutMs, int readTimeoutMs)
            throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
        connection.setUseCaches(false);
        connection.setRequestProperty("Accept", "application/vnd.github+json");

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected HTTP response code: " + responseCode);
            }
            String payload = readUtf8(connection.getInputStream());
            JSONObject object = new JSONObject(payload);
            return object.optString("body", "").trim();
        } catch (org.json.JSONException exception) {
            throw new IOException("Invalid GitHub release payload", exception);
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

    private static String normalizeVersionName(String versionName) {
        if (versionName == null) {
            return "";
        }
        String normalized = versionName.trim();
        if (normalized.startsWith("v") || normalized.startsWith("V")) {
            normalized = normalized.substring(1).trim();
        }
        return normalized;
    }
}
