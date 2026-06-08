package com.dpis.module;

import java.util.ArrayList;
import java.util.List;

final class WechatDpiRoutes {
    private static final Route[] ROUTES = {
            new Route(3120L, "8.0.74", "j65.f"),
            new Route(3100L, "8.0.72", "w45.f"),
            new Route(3080L, "8.0.71", "q35.f"),
            new Route(3060L, "8.0.70", "d25.f"),
            new Route(3040L, "8.0.69", "az4.f"),
            new Route(2460L, "8.0.42", "hy3.d")
    };

    private WechatDpiRoutes() {
    }

    static Route forVersionCode(long versionCode) {
        if (versionCode <= 0L) {
            return null;
        }
        for (Route route : ROUTES) {
            if (route.versionCode == versionCode) {
                return route;
            }
        }
        return null;
    }

    static boolean supportsVersionCode(long versionCode) {
        return forVersionCode(versionCode) != null;
    }

    static List<Route> all() {
        ArrayList<Route> routes = new ArrayList<>(ROUTES.length);
        for (Route route : ROUTES) {
            routes.add(route);
        }
        return routes;
    }

    static final class Route {
        final long versionCode;
        final String versionName;
        final String className;

        private Route(long versionCode, String versionName, String className) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.className = className;
        }

        String routeKey() {
            return className;
        }
    }
}
