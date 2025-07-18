package com.thomas.cache.handler.caffeine

import java.util.stream.Stream
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class CaffeineCacheHandlerTest {

    private lateinit var cacheHandler: CaffeineCacheHandler
    private lateinit var properties: CaffeineCacheHandlerProperties

    companion object {
        private val DEFAULT_PROPERTIES = CaffeineCacheHandlerProperties(
            initialCacheCapacity = 100,
            maximumCacheSize = 1000,
            expireWriteSeconds = 10L,
            expireAccessSeconds = 5L
        )

        @JvmStatic
        fun differentDataTypesProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of("string-key", "string-value", String::class.java),
                Arguments.of("int-key", 42, Integer::class.java),
                Arguments.of("long-key", 42L, java.lang.Long::class.java),
                Arguments.of("double-key", 3.14, java.lang.Double::class.java),
                Arguments.of("boolean-key", true, java.lang.Boolean::class.java),
                Arguments.of("list-key", listOf("a", "b", "c"), List::class.java),
                Arguments.of("map-key", mapOf("key" to "value"), Map::class.java),
                Arguments.of("object-key", TestSimpleObject("test"), TestSimpleObject::class.java),
                Arguments.of("null-key", null, null)
            )
        }
    }

    @BeforeEach
    fun setUp() {
        properties = DEFAULT_PROPERTIES
        cacheHandler = CaffeineCacheHandler(properties)
    }

    @AfterEach
    fun tearDown() {
        runTest {
            cacheHandler.clear()
        }
    }

    @Test
    fun `Should create cache handler with valid properties`() {
        val customProperties = CaffeineCacheHandlerProperties(
            initialCacheCapacity = 50,
            maximumCacheSize = 500,
            expireWriteSeconds = 3600L,
            expireAccessSeconds = 1800L
        )

        val customCacheHandler = CaffeineCacheHandler(customProperties)

        assertNotNull(customCacheHandler)
    }

    @Test
    fun `Should set and get string value successfully`() = runTest {
        val key = "test-key"
        val value = "test-value"

        cacheHandler.set(key, value)
        val retrievedValue = cacheHandler.get<String>(key)

        assertEquals(value, retrievedValue)
    }

    @Test
    fun `Should set and get null value successfully`() = runTest {
        val key = "null-key"
        val value: String? = null

        cacheHandler.set(key, value)
        val retrievedValue = cacheHandler.get<String>(key)

        assertNull(retrievedValue)
    }

    @Test
    fun `Should return null for non-existent key`() = runTest {
        val nonExistentKey = "non-existent-key"

        val retrievedValue = cacheHandler.get<String>(nonExistentKey)

        assertNull(retrievedValue)
    }

    @Test
    fun `Should delete existing key successfully`() = runTest {
        val key = "delete-key"
        val value = "delete-value"
        cacheHandler.set(key, value)

        cacheHandler.delete(key)
        val retrievedValue = cacheHandler.get<String>(key)

        assertNull(retrievedValue)
    }

    @Test
    fun `Should handle deletion of non-existent key gracefully`() = runTest {
        val nonExistentKey = "non-existent-delete-key"

        try {
            cacheHandler.delete(nonExistentKey)
        } catch (e: Exception) {
            fail("Should not throw exception when deleting non-existent key: ${e.message}")
        }
    }

    @Test
    fun `Should clear all cache entries successfully`() = runTest {
        val entries = mapOf(
            "key1" to "value1",
            "key2" to "value2",
            "key3" to "value3"
        )

        entries.forEach { (key, value) ->
            cacheHandler.set(key, value)
        }

        cacheHandler.clear()

        entries.keys.forEach { key ->
            assertNull(cacheHandler.get<String>(key))
        }
    }

    @ParameterizedTest
    @MethodSource("differentDataTypesProvider")
    fun `Should handle different data types correctly`(
        key: String,
        value: Any?,
        expectedType: Class<*>?
    ) = runTest {
        cacheHandler.set(key, value)
        val retrievedValue = cacheHandler.get<Any>(key)

        if (value == null) {
            assertNull(retrievedValue)
        } else {
            assertNotNull(retrievedValue)
            assertTrue(expectedType!!.isInstance(retrievedValue))
            assertEquals(value, retrievedValue)
        }
    }

    @Test
    fun `Should handle complex objects correctly`() = runTest {
        val key = "complex-object"
        val value = TestComplexObject(
            id = 123,
            name = "Test Object",
            tags = listOf("tag1", "tag2", "tag3"),
            metadata = mapOf("key1" to "value1", "key2" to "value2")
        )

        cacheHandler.set(key, value)
        val retrievedValue = cacheHandler.get<TestComplexObject>(key)

        assertNotNull(retrievedValue)
        assertEquals(value.id, retrievedValue!!.id)
        assertEquals(value.name, retrievedValue.name)
        assertEquals(value.tags, retrievedValue.tags)
        assertEquals(value.metadata, retrievedValue.metadata)
    }

    @Test
    fun `Should handle concurrent operations correctly`() = runTest {
        val numberOfOperations = 100
        val keyPrefix = "concurrent-key-"
        val valuePrefix = "concurrent-value-"

        val writeJobs = (1..numberOfOperations).map { i ->
            async { cacheHandler.set("$keyPrefix$i", "$valuePrefix$i") }
        }
        writeJobs.awaitAll()

        (1..numberOfOperations).forEach { i ->
            val retrievedValue = cacheHandler.get<String>("$keyPrefix$i")
            assertEquals("$valuePrefix$i", retrievedValue)
        }

        val readJobs = (1..numberOfOperations).map { i ->
            async {
                cacheHandler.get<String>("$keyPrefix$i")
            }
        }
        val readResults = readJobs.awaitAll()

        readResults.forEachIndexed { index, value ->
            assertEquals("$valuePrefix${index + 1}", value)
        }
    }

    @Test
    fun `Should handle concurrent mixed operations correctly`() = runTest {
        val key = "mixed-ops-key"
        val initialValue = "initial-value"
        cacheHandler.set(key, initialValue)

        val operations = (1..50).map { i ->
            async {
                when (i % 4) {
                    0 -> cacheHandler.set(key, "updated-value-$i")
                    1 -> cacheHandler.get<String>(key)
                    2 -> cacheHandler.delete(key)
                    3 -> cacheHandler.set(key, "new-value-$i")
                    else -> null
                }
            }
        }

        try {
            operations.awaitAll()
        } catch (e: Exception) {
            fail("Should not throw exception when handling concurrent mixed operations: ${e.message}")
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "key with spaces", "key-with-dashes", "key_with_underscores", "key123", "UPPERCASE_KEY"])
    fun `Should handle different key formats correctly`(key: String) = runTest {
        val value = "test-value-for-$key"

        cacheHandler.set(key, value)
        val retrievedValue = cacheHandler.get<String>(key)

        assertEquals(value, retrievedValue)
    }

    @Test
    fun `Should handle very long keys and values`() = runTest {
        val longKey = "key-" + "x".repeat(1000)
        val longValue = "value-" + "y".repeat(10000)

        cacheHandler.set(longKey, longValue)
        val retrievedValue = cacheHandler.get<String>(longKey)

        assertEquals(longValue, retrievedValue)
    }

    @Test
    fun `Should handle special characters in keys and values`() = runTest {
        val specialKey = "key-çãõ!@#$%^&*()[]{}|\\:;\"'<>,.?/"
        val specialValue = "value-çãõ!@#$%^&*()[]{}|\\:;\"'<>,.?/"

        cacheHandler.set(specialKey, specialValue)
        val retrievedValue = cacheHandler.get<String>(specialKey)

        assertEquals(specialValue, retrievedValue)
    }

    @Test
    fun `Should overwrite existing values correctly`() = runTest {
        val key = "overwrite-key"
        val originalValue = "original-value"
        val newValue = "new-value"

        cacheHandler.set(key, originalValue)
        assertEquals(originalValue, cacheHandler.get<String>(key))

        cacheHandler.set(key, newValue)
        val finalValue = cacheHandler.get<String>(key)

        assertEquals(newValue, finalValue)
        assertNotEquals(originalValue, finalValue)
    }

    @Test
    fun `Should overwrite with different data types`() = runTest {
        val key = "type-change-key"
        val stringValue = "string-value"
        val intValue = 42

        cacheHandler.set(key, stringValue)
        assertEquals(stringValue, cacheHandler.get<String>(key))

        cacheHandler.set(key, intValue)
        val finalValue = cacheHandler.get<Int>(key)

        assertEquals(intValue, finalValue)
        assertNotEquals(stringValue, finalValue)
    }

    @Test
    fun `Should handle large number of operations efficiently`() = runTest {
        val numberOfOperations = 1000
        val startTime = System.currentTimeMillis()

        repeat(numberOfOperations) { i ->
            cacheHandler.set("perf-key-$i", "perf-value-$i")
        }

        repeat(numberOfOperations) { i ->
            cacheHandler.get<String>("perf-key-$i")
        }

        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime

        assertTrue(totalTime < 5000, "Operations took too long: ${totalTime}ms")
    }

    data class TestComplexObject(
        val id: Int,
        val name: String,
        val tags: List<String>,
        val metadata: Map<String, String>
    )

    data class TestSimpleObject(
        val value: String
    )

}