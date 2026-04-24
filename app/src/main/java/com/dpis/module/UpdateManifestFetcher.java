package com.dpis.module;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

final class UpdateManifestFetcher {
    private UpdateManifestFetcher() {
    }

    static StartupUpdateManifest fetch(String manifestUrl,
            int connectTimeoutMs,
            int readTimeoutMs) throws IOException, JSONException {
        HttpURLConnection connection = (HttpURLConnection) new URL(manifestUrl).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
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
            return new StartupUpdateManifest(versionName, versionCode, apkUrl, releasePage);
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
}
