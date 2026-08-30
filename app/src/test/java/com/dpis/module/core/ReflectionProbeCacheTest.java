package com.dpis.module;

import com.dpis.module.runtime.systemserver.ReflectionProbeCache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.Test;

public class ReflectionProbeCacheTest {
    @Test
    public void findsFieldsDeclaredOnParentClasses() throws Exception {
        ReflectionProbeCache cache = new ReflectionProbeCache();
        Child child = new Child();

        Field field = cache.findField(Child.class, "parentValue");

        assertNotNull(field);
        assertEquals("parent", field.get(child));
    }

    @Test
    public void returnsStableMissForMissingField() {
        ReflectionProbeCache cache = new ReflectionProbeCache();

        assertNull(cache.findField(Child.class, "missing"));
        assertNull(cache.findField(Child.class, "missing"));
    }

    @Test
    public void keepsFieldOrderFromChildToParent() {
        ReflectionProbeCache cache = new ReflectionProbeCache();

        List<Field> fields = cache.getAllFields(Child.class);

        assertEquals("childValue", fields.get(0).getName());
        assertTrue(fields.stream().anyMatch(field -> "parentValue".equals(field.getName())));
        assertThrows(UnsupportedOperationException.class, () -> fields.add(fields.get(0)));
    }

    @Test
    public void findsNoArgMethodsByClass() throws Exception {
        ReflectionProbeCache cache = new ReflectionProbeCache();

        Method childMethod = cache.findNoArgMethod(Child.class, "name");
        Method parentMethod = cache.findNoArgMethod(Parent.class, "name");

        assertNotNull(childMethod);
        assertNotNull(parentMethod);
        assertEquals("child", childMethod.invoke(new Child()));
        assertEquals("parent", parentMethod.invoke(new Parent()));
    }

    private static class Parent {
        private final String parentValue = "parent";

        public String name() {
            return "parent";
        }
    }

    private static final class Child extends Parent {
        private final String childValue = "child";

        @Override
        public String name() {
            return "child";
        }
    }
}
