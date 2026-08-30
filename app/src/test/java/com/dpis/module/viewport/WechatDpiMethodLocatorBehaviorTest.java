package com.dpis.module;

import com.dpis.module.quirks.WechatDpiMethodLocator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.res.Configuration;
import android.util.DisplayMetrics;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WechatDpiMethodLocatorBehaviorTest {
    @Test
    public void staticRouteUsesVerifiedMethodRolesForWechat8074() {
        WechatDpiMethodLocator.Result result = WechatDpiMethodLocator.locate(
                getClass().getClassLoader(), null, 3120L);

        assertEquals(WechatDpiMethodLocator.Source.STATIC_ROUTE, result.source);
        assertEquals(2, result.methods.size());
        assertEquals("d", result.methods.get(0).getName());
        assertEquals("e", result.methods.get(1).getName());
    }

    @Test
    public void staticOnlyLookupDoesNotRunDexKitForUnknownVersions() {
        WechatDpiMethodLocator.Result result = WechatDpiMethodLocator.locate(
                getClass().getClassLoader(), null, 9999L, false);

        assertEquals(WechatDpiMethodLocator.Source.STATIC_ROUTE, result.source);
        assertTrue(result.methods.isEmpty());
        assertTrue(result.failure.contains("unsupported versionCode=9999"));
    }

    @Test
    public void densityManagerMethodsKeepsDisplayMetricsGettersOnly() {
        List<Method> methods = WechatDpiMethodLocator.densityManagerMethods(
                FakeDensityManager.class);

        assertEquals(2, methods.size());
        Set<String> names = new HashSet<>();
        for (Method method : methods) {
            names.add(method.getName());
        }
        assertEquals(Set.of("getterA", "getterB"), names);
    }

    private static final class FakeDensityManager {
        public DisplayMetrics getterA() {
            return null;
        }

        public DisplayMetrics getterB() {
            return null;
        }

        public void mutator(Configuration configuration, DisplayMetrics metrics) {
        }

        public static int targetGetter() {
            return 0;
        }

        public static void targetSetter(int dpi) {
        }

        public int wrongReturnType() {
            return 0;
        }
    }
}
