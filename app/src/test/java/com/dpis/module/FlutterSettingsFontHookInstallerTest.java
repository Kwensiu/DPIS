package com.dpis.module;

import com.dpis.module.runtime.font.FlutterSettingsFontHookInstaller;

import android.util.DisplayMetrics;

import org.junit.Test;

import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class FlutterSettingsFontHookInstallerTest {
        @Test
        public void recognisesFlutterSettingsMessageBuilderClassOnly() {
                assertTrue(FlutterSettingsFontHookInstaller.isSettingsChannelClassForTest(
                                "io.flutter.embedding.engine.systemchannels.SettingsChannel"));
                assertTrue(FlutterSettingsFontHookInstaller.isMessageBuilderClassForTest(
                                "io.flutter.embedding.engine.systemchannels.SettingsChannel$MessageBuilder"));
                assertTrue(FlutterSettingsFontHookInstaller.isSettingsMessageBuilderClassForTest(
                                "io.flutter.embedding.engine.systemchannels.SettingsChannel$MessageBuilder"));
                assertFalse(FlutterSettingsFontHookInstaller.isMessageBuilderClassForTest(
                                "io.flutter.embedding.android.FlutterView"));
                assertTrue(FlutterSettingsFontHookInstaller.isFlutterViewClassForTest(
                                "io.flutter.embedding.android.FlutterView"));
                assertTrue(FlutterSettingsFontHookInstaller.isFlutterFragmentClassForTest(
                                "io.flutter.embedding.android.FlutterFragment"));
                assertTrue(FlutterSettingsFontHookInstaller.isFlutterJniClassForTest(
                                "io.flutter.embedding.engine.FlutterJNI"));
                assertTrue(FlutterSettingsFontHookInstaller.isFlutterEngineTypeNameForTest(
                                "io.flutter.embedding.engine.FlutterEngine"));
                assertFalse(FlutterSettingsFontHookInstaller.isFlutterEngineTypeNameForTest(
                                "io.flutter.embedding.android.FlutterView"));
        }

        @Test
        public void sourceContainsAppClassLoaderRetryMethod() throws Exception {
                String source = readSource("src/main/java/com/dpis/module/runtime/font/FlutterSettingsFontHookInstaller.java");

                assertTrue(source.contains("static void retryWithAppClassLoader("));
                assertTrue(source.contains("APP_CLASSLOADER_RETRY_ATTEMPTED"));
                assertTrue(source.contains("bridgeProbe(\"DPIS_FONT Flutter semantic app-classloader retry:"));
                assertTrue(source.contains(
                                "tryHookMessageBuilder(xposed, packageName, fontScale.effective, fontScale.targetPercent,"));
                assertTrue(source.contains(
                                "tryHookFlutterJni(xposed, packageName, fontScale.effective, fontScale.targetPercent,"));
        }

        @Test
        public void sourceContainsTypefaceOnlyDefaultFamilyOverlayRoute() throws Exception {
                String source = readSource(
                                "src/main/java/com/dpis/module/runtime/font/FlutterSettingsFontHookInstaller.java");

                assertTrue(source.contains("installTypefaceProbe("));
                assertTrue(source.contains("runBundleAndSnapshotFromLibrary"));
                assertTrue(source.contains("installTypefaceDefaultFamilyOverlay("));
                assertTrue(source.contains("appendDefaultRobotoFamily("));
                assertTrue(source.contains("dpis/typeface.ttf"));
                assertTrue(source.contains("\"addAssetPath\""));
        }

        @Test
        public void sourceContainsFlutterViewAttachBridgeForLateSemanticHookInstall() throws Exception {
                String source = readSource("src/main/java/com/dpis/module/runtime/font/FlutterSettingsFontHookInstaller.java");

                assertTrue(source.contains("installFlutterViewAttachBridge("));
                assertTrue(source.contains("installBaseDexFindClassHook("));
                assertTrue(source.contains("installLoadedApkClassLoaderHook("));
                assertTrue(source.contains("installContentProviderAttachClassLoaderHook("));
                assertTrue(source.contains("installApplicationAttachClassLoaderHook("));
                assertTrue(source.contains("installApplicationCreateClassLoaderHook("));
                assertFalse(defaultInstallBody(source).contains("installActivityResumeFlutterViewScan("));
                assertFalse(defaultInstallBody(source).contains("installViewRootFlutterViewScan("));
                assertFalse(defaultInstallBody(source).contains("startActiveActivityFlutterViewProbeThread("));
                assertTrue(source.contains("tryHookFlutterFragment("));
                assertTrue(source.contains("bridgeProbe(\"DPIS_FONT Flutter semantic install active:"));
                assertTrue(source.contains("Class.forName(\"dalvik.system.BaseDexClassLoader\""));
                assertTrue(source.contains("baseDexClassLoader.getDeclaredMethod(\"findClass\", String.class)"));
                assertTrue(source.contains("bridgeProbe(\"DPIS_FONT Flutter semantic BaseDex findClass hit:"));
                assertTrue(source.contains("Class.forName(\"android.app.LoadedApk\""));
                assertTrue(source.contains("loadedApkClass.getDeclaredMethod(\"getClassLoader\")"));
                assertTrue(source.contains(
                                "bridgeProbe(\"DPIS_FONT app-classloader-source: loaded-apk-getClassLoader FIRED"));
                assertTrue(source.contains("Application.class.getDeclaredMethod(\"attach\", Context.class)"));
                assertTrue(source.contains("bridgeProbe(\"DPIS_FONT app-classloader-source: application-attach FIRED"));
                assertTrue(source.contains("Application.class.getDeclaredMethod(\"onCreate\")"));
                assertTrue(source.contains(
                                "bridgeProbe(\"DPIS_FONT app-classloader-source: application-onCreate FIRED"));
                assertTrue(source.contains("ContentProvider.class.getDeclaredMethod(\"attachInfo\""));
                assertTrue(source.contains(
                                "bridgeProbe(\"DPIS_FONT app-classloader-source: content-provider-attach FIRED"));
                assertTrue(source.contains("bridgeProbe(\"DPIS_FONT Flutter semantic fragment view hook ready"));
                assertTrue(source.contains("bridgeProbe(\"DPIS_FONT Flutter semantic fragment view created:"));
                assertTrue(source.contains("bridgeProbe(\"DPIS_FONT Flutter semantic settings override:"));
                assertTrue(source.contains("bridgeProbe(\"DPIS_FONT Flutter semantic resend user settings:"));
                assertTrue(source.contains("getDeclaredMethod(\"onViewCreated\", View.class"));
                assertTrue(source.contains("View.class.getDeclaredMethod(\"onAttachedToWindow\")"));
                assertTrue(source.contains("Activity.class.getDeclaredMethod(\"onResume\")"));
                assertTrue(source.contains("viewRootImplClass.getDeclaredMethod(\"performTraversals\")"));
                assertTrue(source.contains("readField(chain.getThisObject(), \"mView\")"));
                assertTrue(source.contains("scanFlutterViews("));
                assertTrue(source.contains("installSemanticHooksFromFlutterObject("));
                assertTrue(source.contains("findFlutterEngine("));
                assertTrue(source.contains("\"flutter-view-attached\""));
                assertTrue(source.contains("\"activity-resume\""));
                assertTrue(source.contains("\"view-root\""));
                assertTrue(source.contains("resendFlutterUserSettings(view, packageName"));
        }

        @Test
        public void recognisesSupportedSettingsMethods() throws Exception {
                Method textScale = FakeMessageBuilder.class.getDeclaredMethod(
                                "setTextScaleFactor", float.class);
                Method displayMetrics = FakeMessageBuilder.class.getDeclaredMethod(
                                "setDisplayMetrics", DisplayMetrics.class);
                Method wrong = FakeMessageBuilder.class.getDeclaredMethod(
                                "setTextScaleFactor", double.class);

                assertTrue(FlutterSettingsFontHookInstaller.isSetTextScaleFactorMethodForTest(textScale));
                assertTrue(FlutterSettingsFontHookInstaller.isSetDisplayMetricsMethodForTest(displayMetrics));
                assertFalse(FlutterSettingsFontHookInstaller.isSetTextScaleFactorMethodForTest(wrong));
        }

        @Test
        public void recognisesFlutterJniPlatformMessageMethods() throws Exception {
                Method dispatch = FakeFlutterJni.class.getDeclaredMethod(
                                "dispatchPlatformMessage", String.class, ByteBuffer.class, int.class, int.class);
                Method nativeDispatch = FakeFlutterJni.class.getDeclaredMethod(
                                "nativeDispatchPlatformMessage", long.class, String.class,
                                ByteBuffer.class, int.class, int.class);
                Method wrong = FakeFlutterJni.class.getDeclaredMethod(
                                "dispatchPlatformMessage", String.class, byte[].class, int.class, int.class);

                assertTrue(FlutterSettingsFontHookInstaller
                                .isPlatformMessageDispatchMethodForTest(dispatch));
                assertTrue(FlutterSettingsFontHookInstaller
                                .isPlatformMessageDispatchMethodForTest(nativeDispatch));
                assertFalse(FlutterSettingsFontHookInstaller
                                .isPlatformMessageDispatchMethodForTest(wrong));
        }

        @Test
        public void rewritesFlutterSettingsJsonTextScaleFactor() {
                byte[] payload = "{\"textScaleFactor\":1.0,\"platformBrightness\":\"light\"}"
                                .getBytes(StandardCharsets.UTF_8);
                ByteBuffer buffer = ByteBuffer.allocateDirect(payload.length);
                buffer.put(payload);
                buffer.flip();

                ByteBuffer adjusted = FlutterSettingsFontHookInstaller
                                .replaceSettingsTextScaleFactorForTest(buffer, payload.length, 3.0f);

                assertNotNull(adjusted);
                byte[] adjustedBytes = new byte[adjusted.remaining()];
                adjusted.get(adjustedBytes);
                String json = new String(adjustedBytes, StandardCharsets.UTF_8);
                assertTrue(json.contains("\"textScaleFactor\":3"));
                assertTrue(json.contains("\"platformBrightness\":\"light\""));
        }

        @Test
        public void displayMetricsClonePreservesDensityAndAppliesTargetScaledDensity() {
                DisplayMetrics metrics = new DisplayMetrics();
                metrics.densityDpi = 480;
                metrics.density = 3.0f;
                metrics.scaledDensity = 3.0f;

                DisplayMetrics adjusted = FlutterSettingsFontHookInstaller.cloneWithTargetScaledDensityForTest(metrics,
                                3.0f);

                assertEquals(480, adjusted.densityDpi);
                assertEquals(3.0f, adjusted.density, 0.0001f);
                assertEquals(9.0f, adjusted.scaledDensity, 0.0001f);
                assertEquals(3.0f, metrics.scaledDensity, 0.0001f);
        }

        @Test
        public void displayMetricsCloneUsesAbsoluteTargetInsteadOfMultiplyingExistingScale() {
                DisplayMetrics metrics = new DisplayMetrics();
                metrics.densityDpi = 480;
                metrics.density = 3.0f;
                metrics.scaledDensity = 6.0f;

                DisplayMetrics adjusted = FlutterSettingsFontHookInstaller.cloneWithTargetScaledDensityForTest(metrics,
                                3.0f);

                assertEquals(9.0f, adjusted.scaledDensity, 0.0001f);
                assertEquals(6.0f, metrics.scaledDensity, 0.0001f);
        }

        private static final class FakeMessageBuilder {
                Object setTextScaleFactor(float textScaleFactor) {
                        return textScaleFactor;
                }

                Object setTextScaleFactor(double textScaleFactor) {
                        return textScaleFactor;
                }

                Object setDisplayMetrics(DisplayMetrics displayMetrics) {
                        return displayMetrics;
                }
        }

        private static final class FakeFlutterJni {
                void dispatchPlatformMessage(String channel, ByteBuffer message,
                                int position, int responseId) {
                }

                void nativeDispatchPlatformMessage(long shellHolder, String channel,
                                ByteBuffer message, int position,
                                int responseId) {
                }

                void dispatchPlatformMessage(String channel, byte[] message,
                                int position, int responseId) {
                }
        }

        private static String readSource(String relativePath) throws Exception {
                return SourceSmokeTestPaths.read(relativePath);
        }

        private static String defaultInstallBody(String source) {
                int start = source.indexOf("static void install(XposedInterface xposed,");
                int end = source.indexOf("private static void installLoadedClassHook", start);
                return source.substring(start, end);
        }
}
