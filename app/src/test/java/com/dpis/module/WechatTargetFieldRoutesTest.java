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

public class WechatTargetFieldRoutesTest {
    @Test
    public void resolvesExactVersionCodeRoutes() {
        assertRoute(3100L, "8.0.72", WechatTargetFieldRoutes.Kind.GETTER, "w45.f",
                "g", null);
        assertRoute(3080L, "8.0.71", WechatTargetFieldRoutes.Kind.GETTER,
                "q35.f", "g", "k");
        assertRoute(3060L, "8.0.70", WechatTargetFieldRoutes.Kind.GETTER, "d25.f",
                "g", null);
        assertRoute(3040L, "8.0.69", WechatTargetFieldRoutes.Kind.GETTER, "az4.f",
                "g", "k");
        assertRoute(2460L, "8.0.42", WechatTargetFieldRoutes.Kind.GETTER, "hy3.d",
                "g", null);
    }

    @Test
    public void rejectsUnknownVersionCodes() {
        assertNull(WechatTargetFieldRoutes.forVersionCode(0L));
        assertNull(WechatTargetFieldRoutes.forVersionCode(-1L));
        assertNull(WechatTargetFieldRoutes.forVersionCode(9999L));

        assertFalse(WechatTargetFieldRoutes.supportsVersionCode(0L));
        assertFalse(WechatTargetFieldRoutes.supportsVersionCode(9999L));
        assertTrue(WechatTargetFieldRoutes.supportsVersionCode(3100L));
    }

    @Test
    public void routeListHasUniqueVersionCodes() {
        List<WechatTargetFieldRoutes.Route> routes = WechatTargetFieldRoutes.all();
        Set<Long> versionCodes = new HashSet<>();
        for (WechatTargetFieldRoutes.Route route : routes) {
            assertTrue("duplicate versionCode " + route.versionCode,
                    versionCodes.add(route.versionCode));
        }
        assertEquals(5, routes.size());
    }

    private static void assertRoute(long versionCode, String versionName,
            WechatTargetFieldRoutes.Kind kind, String className, String memberName,
            String setterName) {
        WechatTargetFieldRoutes.Route route = WechatTargetFieldRoutes.forVersionCode(versionCode);
        assertNotNull(route);
        assertEquals(versionCode, route.versionCode);
        assertEquals(versionName, route.versionName);
        assertEquals(kind, route.kind);
        assertEquals(className, route.className);
        assertEquals(memberName, route.memberName);
        assertEquals(setterName, route.setterName);
        assertEquals(className + "#" + memberName, route.routeKey());
        assertEquals(setterName != null ? className + "#" + setterName : "",
                route.setterRouteKey());
    }
}
