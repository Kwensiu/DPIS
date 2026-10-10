package com.dpis.module;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FontFileProviderSourceSmokeTest {
    @Test
    public void providerOnlyOpensSelectedFaceForCallingPackage() throws Exception {
        String source = SourceSmokeTestPaths.read(
                "src/main/java/com/dpis/module/fonts/file/FontFileProvider.kt");
        String manifest = SourceSmokeTestPaths.read("src/main/AndroidManifest.xml");

        assertTrue(manifest.contains(".fonts.FontFileProvider"));
        assertTrue(manifest.contains("${applicationId}.fonts"));
        assertTrue(source.contains("Binder.getCallingUid()"));
        assertTrue(source.contains("getPackagesForUid(callingUid)"));
        assertTrue(source.contains("DpisApplication.getActiveHookConfigStore(context)"));
        assertFalse(source.contains("createPackageLifecycleConfigStore(context)"));
        assertTrue(source.contains("configStore.getTargetTypefaceId(it)"));
        assertTrue(source.contains("PATH_FACE"));
        assertTrue(source.contains("ParcelFileDescriptor.MODE_READ_ONLY"));
        assertTrue(source.contains("Font files are read-only"));
    }
}
