package com.dpis.module;

import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

final class StartupUpdateDownloadExecutor {
    interface Cancellation {
        boolean isCanceled();
    }

    interface Listener {
        void onConnectionOpened(HttpURLConnection connection, long totalBytes);

        void onProgress(long downloadedBytes, long totalBytes);
    }

    static final class DownloadCanceledException extends IOException {
    }

    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final int bufferSize;
    private final long progressUpdateIntervalMs;

    StartupUpdateDownloadExecutor(int connectTimeoutMs,
                                  int readTimeoutMs,
                                  int bufferSize,
                                  long progressUpdateIntervalMs) {
        if (connectTimeoutMs <= 0 || readTimeoutMs <= 0 || bufferSize <= 0 || progressUpdateIntervalMs <= 0L) {
            throw new IllegalArgumentException("invalid download executor configuration");
        }
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.bufferSize = bufferSize;
        this.progressUpdateIntervalMs = progressUpdateIntervalMs;
    }

    void download(Uri downloadUri,
                  File targetFile,
                  Cancellation cancellation,
                  Listener listener) throws IOException, DownloadCanceledException {
        HttpURLConnection connection = null;
        long totalBytes = 0L;
        long downloadedBytes = 0L;
        try {
            connection = (HttpURLConnection) new URL(downloadUri.toString()).openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(connectTimeoutMs);
            connection.setReadTimeout(readTimeoutMs);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/octet-stream,*/*");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Unexpected HTTP response code: " + responseCode);
            }

            totalBytes = connection.getContentLengthLong();
            if (listener != null) {
                listener.onConnectionOpened(connection, totalBytes);
            }

            try (InputStream inputStream = connection.getInputStream();
                    FileOutputStream outputStream = new FileOutputStream(targetFile)) {
                byte[] buffer = new byte[bufferSize];
                long lastProgressDispatchAt = 0L;

                while (true) {
                    if (isCanceled(cancellation)) {
                        throw new DownloadCanceledException();
                    }
                    int read = inputStream.read(buffer);
                    if (read < 0) {
                        break;
                    }
                    outputStream.write(buffer, 0, read);
                    downloadedBytes += read;

                    long now = System.currentTimeMillis();
                    if (now - lastProgressDispatchAt < progressUpdateIntervalMs && totalBytes > 0L) {
                        continue;
                    }
                    lastProgressDispatchAt = now;
                    if (listener != null) {
                        listener.onProgress(downloadedBytes, totalBytes);
                    }
                }
                outputStream.flush();
            }

            if (isCanceled(cancellation)) {
                throw new DownloadCanceledException();
            }
            if (listener != null) {
                listener.onProgress(downloadedBytes, totalBytes);
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static boolean isCanceled(Cancellation cancellation) {
        return cancellation != null && cancellation.isCanceled();
    }
}
