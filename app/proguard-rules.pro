-adaptresourcefilenames
-keep class com.dpis.module.ModuleMain
-keep class com.dpis.module.ModuleMain { *; }

-keep class com.dpis.module.LegacyModuleHook
-keep class com.dpis.module.LegacyModuleHook { *; }

-keepclassmembers class com.dpis.module.DpisApplication {
    static void markXposedSelfLoaded();
    boolean xposedSelfLoadedByLegacyConstructorHook;
}

-keepclassmembers class com.dpis.module.ResourcesManagerHookInstaller {
    private static void applyResourceOverrides(android.content.res.Configuration, com.dpis.module.DpiConfigStore, java.lang.String, java.lang.String);
}

-keep class com.dpis.module.HyperOsFlutterFontHookInstaller { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}
