package com.dpis.module;

import java.util.ArrayList;
import java.util.List;

final class WechatTargetFieldRoutes {
    private static final Route[] ROUTES = {
            Route.getter(3100L, "8.0.72", "w45.f", "g"),
            Route.getter(3080L, "8.0.71", "q35.f", "g", "k"),
            Route.getter(3060L, "8.0.70", "d25.f", "g"),
            Route.getter(3040L, "8.0.69", "az4.f", "g", "k"),
            Route.getter(2460L, "8.0.42", "hy3.d", "g")
    };

    private WechatTargetFieldRoutes() {
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
        final Kind kind;
        final String className;
        final String memberName;
        final String setterName;

        private Route(long versionCode, String versionName, Kind kind, String className,
                String memberName, String setterName) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.kind = kind;
            this.className = className;
            this.memberName = memberName;
            this.setterName = setterName;
        }

        static Route getter(long versionCode, String versionName, String className,
                String methodName) {
            return getter(versionCode, versionName, className, methodName, null);
        }

        static Route getter(long versionCode, String versionName, String className,
                String methodName, String setterName) {
            return new Route(versionCode, versionName, Kind.GETTER, className, methodName,
                    setterName);
        }

        static Route constructorField(long versionCode, String versionName, String className,
                String fieldName) {
            return new Route(versionCode, versionName, Kind.CONSTRUCTOR_FIELD, className,
                    fieldName, null);
        }

        String routeKey() {
            return className + "#" + memberName;
        }

        String setterRouteKey() {
            return setterName != null && !setterName.isBlank()
                    ? className + "#" + setterName
                    : "";
        }
    }

    enum Kind {
        GETTER,
        CONSTRUCTOR_FIELD
    }
}
