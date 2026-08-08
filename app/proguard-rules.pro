-adaptresourcefilenames
-keepclassmembers class com.dpis.module.DpisApplication {
    static void markXposedSelfLoaded();
    boolean xposedSelfLoadedByLegacyConstructorHook;
}

-keepclassmembers class com.dpis.module.runtime.appprocess.ResourcesManagerHookInstaller {
    public static void applyResourceOverrides(android.content.res.Configuration, com.dpis.module.DpisConfigStore, java.lang.String, java.lang.String);
}

-keep class com.dpis.module.runtime.font.HyperOsFlutterFontHookInstaller { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
