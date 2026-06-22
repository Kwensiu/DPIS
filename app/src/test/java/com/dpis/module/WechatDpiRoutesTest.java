package com.dpis.module;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class WechatDpiRoutesTest {
    @Test
    public void resolvesExactVersionCodeRoutes() {
        assertRoute(3120L, "8.0.74", "j65.f", 5);
        assertRoute(3100L, "8.0.72", "w45.f");
        assertRoute(3080L, "8.0.71", "q35.f");
        assertRoute(3060L, "8.0.70", "d25.f");
        assertRoute(3040L, "8.0.69", "az4.f");
        assertRoute(2460L, "8.0.42", "hy3.d");
    }

    @Test
    public void recordsExactMethodTargetsForWechat8074() {
        WechatDpiRoutes.Route route = WechatDpiRoutes.forVersionCode(3120L);

        assertNotNull(route);
        assertEquals("d", route.densityMethodTargets[0].methodName);
        assertEquals(WechatDpiRoutes.MethodTarget.Kind.DISPLAY_METRICS_GETTER,
                route.densityMethodTargets[0].kind);
        assertEquals("e", route.densityMethodTargets[1].methodName);
        assertEquals(WechatDpiRoutes.MethodTarget.Kind.DISPLAY_METRICS_GETTER,
                route.densityMethodTargets[1].kind);
        assertEquals("g", route.densityMethodTargets[2].methodName);
        assertEquals(WechatDpiRoutes.MethodTarget.Kind.TARGET_FIELD_GETTER,
                route.densityMethodTargets[2].kind);
        assertEquals("k", route.densityMethodTargets[3].methodName);
        assertEquals(WechatDpiRoutes.MethodTarget.Kind.TARGET_FIELD_SETTER,
                route.densityMethodTargets[3].kind);
        assertEquals("l", route.densityMethodTargets[4].methodName);
        assertEquals(WechatDpiRoutes.MethodTarget.Kind.DISPLAY_METRICS_MUTATOR,
                route.densityMethodTargets[4].kind);
        assertTrue(route.bottomTabIconScaleEnabled);
    }

    @Test
    public void rejectsUnknownVersionCodes() {
        assertNull(WechatDpiRoutes.forVersionCode(0L));
        assertNull(WechatDpiRoutes.forVersionCode(-1L));
        assertNull(WechatDpiRoutes.forVersionCode(9999L));

        assertFalse(WechatDpiRoutes.supportsVersionCode(0L));
        assertFalse(WechatDpiRoutes.supportsVersionCode(9999L));
        assertTrue(WechatDpiRoutes.supportsVersionCode(3100L));
    }

    @Test
    public void routeListHasUniqueVersionCodes() {
        List<WechatDpiRoutes.Route> routes = WechatDpiRoutes.all();
        Set<Long> versionCodes = new HashSet<>();
        for (WechatDpiRoutes.Route route : routes) {
            assertTrue("duplicate versionCode " + route.versionCode,
                    versionCodes.add(route.versionCode));
        }
        assertEquals(6, routes.size());
    }

    private static void assertRoute(long versionCode, String versionName,
            String className) {
        assertRoute(versionCode, versionName, className, 0);
    }

    private static void assertRoute(long versionCode, String versionName,
            String className, int methodTargetCount) {
        WechatDpiRoutes.Route route = WechatDpiRoutes.forVersionCode(versionCode);
        assertNotNull(route);
        assertEquals(versionCode, route.versionCode);
        assertEquals(versionName, route.versionName);
        assertEquals(className, route.className);
        assertEquals(className, route.routeKey());
        assertEquals(methodTargetCount, route.densityMethodTargets.length);
        if (versionCode != 3120L) {
            assertFalse(route.bottomTabIconScaleEnabled);
        }
    }
}
