package com.dpis.module

import com.dpis.module.runtime.systemserver.reflection.ReflectionProbeCache
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Field
import java.lang.reflect.Method

class ReflectionProbeCacheTest {
    @Test
    fun findsFieldsDeclaredOnParentClasses() {
        val cache = ReflectionProbeCache()
        val child = Child()

        val field = cache.findField(Child::class.java, "parentValue")

        assertNotNull(field)
        assertEquals("parent", field!!.apply { isAccessible = true }.get(child))
    }

    @Test
    fun returnsStableMissForMissingField() {
        val cache = ReflectionProbeCache()

        assertNull(cache.findField(Child::class.java, "missing"))
        assertNull(cache.findField(Child::class.java, "missing"))
    }

    @Test
    fun keepsFieldOrderFromChildToParent() {
        val cache = ReflectionProbeCache()

        val fields: List<Field> = cache.getAllFields(Child::class.java)

        assertEquals("childValue", fields[0].name)
        assertTrue(fields.any { it.name == "parentValue" })
        assertThrows(UnsupportedOperationException::class.java) { (fields as MutableList).add(fields[0]) }
    }

    @Test
    fun findsNoArgMethodsByClass() {
        val cache = ReflectionProbeCache()

        val childMethod: Method? = cache.findNoArgMethod(Child::class.java, "name")
        val parentMethod: Method? = cache.findNoArgMethod(Parent::class.java, "name")

        assertNotNull(childMethod)
        assertNotNull(parentMethod)
        assertEquals("child", childMethod!!.invoke(Child()))
        assertEquals("parent", parentMethod!!.invoke(Parent()))
    }

    private open class Parent {
        @Suppress("unused")
        private val parentValue = "parent"

        open fun name(): String = "parent"
    }

    private class Child : Parent() {
        @Suppress("unused")
        private val childValue = "child"

        override fun name(): String = "child"
    }
}
