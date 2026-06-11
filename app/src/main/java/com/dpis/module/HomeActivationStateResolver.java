package com.dpis.module;

import io.github.libxposed.service.XposedService;

final class HomeActivationStateResolver {
    interface ServiceApiResolver {
        int apiVersion(XposedService service);
    }

    interface ModernServiceProbe {
        boolean hasModernService();
    }

    private static final ServiceApiResolver DEFAULT_SERVICE_API_RESOLVER =
            XposedService::getApiVersion;
    private static ServiceApiResolver serviceApiResolver = DEFAULT_SERVICE_API_RESOLVER;
    private static final ModernServiceProbe DEFAULT_MODERN_SERVICE_PROBE =
            HomeActivationStateResolver::hasModernLibXposedService;
    private static ModernServiceProbe modernServiceProbe = DEFAULT_MODERN_SERVICE_PROBE;

    private HomeActivationStateResolver() {
    }

    static boolean isActivatedForHome() {
        boolean libXposedService = hasModernServiceForHome();
        boolean selfLoaded = DpisApplication.isXposedSelfLoaded();
        boolean activated = libXposedService || selfLoaded;
        DpisLog.i("home activation resolved: libxposedService=" + libXposedService
                + ", selfLoaded=" + selfLoaded
                + ", activated=" + activated);
        return activated;
    }

    static boolean hasModernLibXposedService() {
        XposedService service = DpisApplication.getXposedService();
        if (service == null) {
            return false;
        }
        try {
            return isModernLibXposedServiceApi(serviceApiResolver.apiVersion(service));
        } catch (RuntimeException exception) {
            return false;
        }
    }

    static boolean isModernLibXposedServiceApi(int apiVersion) {
        return apiVersion >= 101;
    }

    static void setServiceApiResolverForTest(ServiceApiResolver resolver) {
        serviceApiResolver = resolver != null ? resolver : DEFAULT_SERVICE_API_RESOLVER;
    }

    static void setModernServiceProbeForTest(ModernServiceProbe probe) {
        modernServiceProbe = probe != null ? probe : DEFAULT_MODERN_SERVICE_PROBE;
    }

    private static boolean hasModernServiceForHome() {
        try {
            return modernServiceProbe.hasModernService();
        } catch (RuntimeException exception) {
            return false;
        }
    }
}
