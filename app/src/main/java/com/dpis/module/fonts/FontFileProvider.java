package com.dpis.module.fonts;

import com.dpis.module.BuildConfig;
import com.dpis.module.ConfigStoreFactory;
import com.dpis.module.DpisApplication;
import com.dpis.module.DpisConfigStore;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

/**
 * Provides a read-only descriptor for the one FontFace currently selected by a target app.
 * It deliberately exposes neither catalog enumeration nor arbitrary app-private paths.
 */
public final class FontFileProvider extends ContentProvider {
    private static final String PATH_FACE = "face";

    public static Uri buildFaceUri(String typefaceId) {
        return new Uri.Builder()
                .scheme("content")
                .authority(BuildConfig.APPLICATION_ID + ".fonts")
                .appendPath(PATH_FACE)
                .appendPath(typefaceId)
                .build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return "font/*";
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("Font files are read-only");
        }
        String typefaceId = resolveTypefaceId(uri);
        Context context = getContext();
        if (context == null || typefaceId == null || !isCallerAuthorized(context, typefaceId)) {
            throw new FileNotFoundException("Font face is unavailable for caller");
        }
        FontLibraryStore store = ConfigStoreFactory.createLocalUiFontLibraryStore(context, null);
        File file = store.resolveFontFile(typefaceId);
        if (file == null || !file.isFile()) {
            throw new FileNotFoundException("Font file is missing");
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private static String resolveTypefaceId(Uri uri) {
        if (uri == null) {
            return null;
        }
        List<String> segments = uri.getPathSegments();
        if (segments.size() != 2 || !PATH_FACE.equals(segments.get(0))) {
            return null;
        }
        String typefaceId = segments.get(1);
        return typefaceId == null || typefaceId.isBlank() ? null : typefaceId;
    }

    private static boolean isCallerAuthorized(Context context, String typefaceId) {
        int callingUid = Binder.getCallingUid();
        if (callingUid == android.os.Process.myUid()) {
            return true;
        }
        PackageManager packageManager = context.getPackageManager();
        String[] packages = packageManager.getPackagesForUid(callingUid);
        if (packages == null || packages.length == 0) {
            return false;
        }
        // The provider lives in DPIS's process, so use the same remote-backed store as the
        // editor. Reading a locally cached XML mirror can otherwise authorize the previous face
        // after the user switches a package to a new one.
        DpisConfigStore configStore = DpisApplication.getActiveHookConfigStore(context);
        if (configStore == null) {
            return false;
        }
        for (String packageName : packages) {
            if (typefaceId.equals(configStore.getTargetTypefaceId(packageName))) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
            @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new UnsupportedOperationException("Font files are read-only");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("Font files are read-only");
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
            @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("Font files are read-only");
    }
}
