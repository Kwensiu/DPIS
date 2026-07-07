package com.dpis.module.home;

import io.github.libxposed.service.XposedService;

public final class HomeActivationStateResolver {
    private HomeActivationStateResolver() {
    }

    public static boolean isActivatedForHome(
            boolean hasModernLibXposedService,
            boolean selfLoaded) {
        return hasModernLibXposedService || selfLoaded;
    }

    public static boolean hasModernLibXposedService(XposedService service) {
        if (service == null) {
            return false;
        }
        try {
            return isModernLibXposedServiceApi(service.getApiVersion());
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static boolean isModernLibXposedServiceApi(int apiVersion) {
        return apiVersion >= 101;
    }
}
