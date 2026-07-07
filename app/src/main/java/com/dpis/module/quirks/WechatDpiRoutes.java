package com.dpis.module.quirks;

import java.util.ArrayList;
import java.util.List;

public final class WechatDpiRoutes {
    private static final Route[] ROUTES = {
            wechat8074Route(),
            route(3100L, "8.0.72", "w45.f"),
            route(3080L, "8.0.71", "q35.f"),
            route(3060L, "8.0.70", "d25.f"),
            route(3040L, "8.0.69", "az4.f"),
            route(2460L, "8.0.42", "hy3.d")
    };

    private WechatDpiRoutes() {
    }

    public static Route forVersionCode(long versionCode) {
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

    public static boolean supportsVersionCode(long versionCode) {
        return forVersionCode(versionCode) != null;
    }

    public static List<Route> all() {
        ArrayList<Route> routes = new ArrayList<>(ROUTES.length);
        for (Route route : ROUTES) {
            routes.add(route);
        }
        return routes;
    }

    public static boolean matchesClassName(String className) {
        if (className == null || className.isBlank()) {
            return false;
        }
        for (Route route : ROUTES) {
            if (className.equals(route.className)) {
                return true;
            }
        }
        return false;
    }

    private static Route wechat8074Route() {
        return routeWithBottomTabIconScale(3120L, "8.0.74", "j65.f",
                MethodTarget.displayMetricsGetter("d"),
                MethodTarget.displayMetricsGetter("e"));
        // Historical 8.0.74 route shape kept for reference only:
        // MethodTarget.targetFieldGetter("g")
        // MethodTarget.targetFieldSetter("k")
        // MethodTarget.displayMetricsMutator("l")
    }

    private static Route route(long versionCode, String versionName, String className,
            MethodTarget... densityMethodTargets) {
        return new Route(versionCode, versionName, className, false, densityMethodTargets);
    }

    private static Route routeWithBottomTabIconScale(long versionCode, String versionName,
            String className, MethodTarget... densityMethodTargets) {
        return new Route(versionCode, versionName, className, true, densityMethodTargets);
    }

    public static final class Route {
        public final long versionCode;
        public final String versionName;
        public final String className;
        public final boolean bottomTabIconScaleEnabled;
        public final MethodTarget[] densityMethodTargets;

        private Route(long versionCode, String versionName, String className,
                boolean bottomTabIconScaleEnabled,
                MethodTarget... densityMethodTargets) {
            this.versionCode = versionCode;
            this.versionName = versionName;
            this.className = className;
            this.bottomTabIconScaleEnabled = bottomTabIconScaleEnabled;
            this.densityMethodTargets = densityMethodTargets != null
                    ? densityMethodTargets.clone()
                    : new MethodTarget[0];
        }

        public String routeKey() {
            return className;
        }

        boolean hasDensityMethodTargets() {
            return densityMethodTargets.length > 0;
        }
    }

    public static final class MethodTarget {
        public final String methodName;
        public final Kind kind;

        private MethodTarget(String methodName, Kind kind) {
            this.methodName = methodName;
            this.kind = kind;
        }

        static MethodTarget displayMetricsGetter(String methodName) {
            return new MethodTarget(methodName, Kind.DISPLAY_METRICS_GETTER);
        }

        static MethodTarget displayMetricsMutator(String methodName) {
            return new MethodTarget(methodName, Kind.DISPLAY_METRICS_MUTATOR);
        }

        static MethodTarget targetFieldGetter(String methodName) {
            return new MethodTarget(methodName, Kind.TARGET_FIELD_GETTER);
        }

        static MethodTarget targetFieldSetter(String methodName) {
            return new MethodTarget(methodName, Kind.TARGET_FIELD_SETTER);
        }

        public enum Kind {
            DISPLAY_METRICS_GETTER,
            DISPLAY_METRICS_MUTATOR,
            TARGET_FIELD_GETTER,
            TARGET_FIELD_SETTER
        }
    }
}
