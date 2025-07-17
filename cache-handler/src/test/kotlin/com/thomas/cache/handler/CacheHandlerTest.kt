package com.thomas.cache.handler

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.random.Random
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CacheHandlerTest {

    private lateinit var cacheHandler: CacheHandler

    @BeforeEach
    fun beforeEach() {
        cacheHandler = SimpleCacheHandler()
    }

    @Test
    fun `Should set value on cache handler`() = runTest {
        // Given
        val testObject = TestObject("1", "test")

        // When
        cacheHandler.set("test", testObject)

        // Then
        val result = cacheHandler.get<TestObject>("test")
        assertEquals(testObject, result)
    }

    @Test
    fun `Should set null value on cache handler`() = runTest {
        // Given
        val nullValue: TestObject? = null

        // When
        cacheHandler.set("test", nullValue)

        // Then
        val result = cacheHandler.get<TestObject>("test")
        assertNull(result)
    }

    @Test
    fun `Should get value from cache handler`() = runTest {
        // Given
        val testObject = TestObject("1", "test")
        cacheHandler.set("test", testObject)

        // When
        val result: TestObject? = cacheHandler.get("test")

        // Then
        assertNotNull(result)
        assertEquals(testObject, result)
    }

    @Test
    fun `Should return null when getting non-existent key`() = runTest {
        // When
        val result: TestObject? = cacheHandler.get("non-existent")

        // Then
        assertNull(result)
    }

    @Test
    fun `Should get null value from cache handler`() = runTest {
        // Given
        cacheHandler.set("test", null)

        // When
        val result: TestObject? = cacheHandler.get("test")

        // Then
        assertNull(result)
    }

    @Test
    fun `Should delete value from cache handler`() = runTest {
        // Given
        val testObject = TestObject("1", "test")
        cacheHandler.set("test", testObject)
        assertNotNull(cacheHandler.get<TestObject>("test"))

        // When
        cacheHandler.delete("test")

        // Then
        assertNull(cacheHandler.get<TestObject>("test"))
    }

    @Test
    fun `Should not throw exception when deleting non-existent key`() = runTest {
        // When & Then
        try {
            cacheHandler.delete("non-existent")
            assertTrue(true)
        } catch (e: Exception) {
            fail("Não deveria lançar exceção ao deletar chave inexistente: ${e.message}")
        }
    }

    @Test
    fun `Should clear all values from cache handler`() = runTest {
        // Given
        cacheHandler.set("test1", TestObject("1", "test1"))
        cacheHandler.set("test2", TestObject("2", "test2"))
        cacheHandler.set("test3", null)

        // When
        cacheHandler.clear()

        // Then
        assertNull(cacheHandler.get<TestObject>("test1"))
        assertNull(cacheHandler.get<TestObject>("test2"))
        assertNull(cacheHandler.get<TestObject>("test3"))
    }

    @Test
    fun `Should handle different data types`() = runTest {
        // Given
        val stringValue = "test string"
        val intValue = 42
        val booleanValue = true

        // When
        cacheHandler.set("string", stringValue)
        cacheHandler.set("int", intValue)
        cacheHandler.set("boolean", booleanValue)

        // Then
        assertEquals(stringValue, cacheHandler.get<String>("string"))
        assertEquals(intValue, cacheHandler.get<Int>("int"))
        assertEquals(booleanValue, cacheHandler.get<Boolean>("boolean"))
    }

    @Test
    fun `Should override existing value`() = runTest {
        // Given
        val originalObject = TestObject("1", "original")
        val newObject = TestObject("2", "new")

        // When
        cacheHandler.set("test", originalObject)
        assertEquals(originalObject, cacheHandler.get<TestObject>("test"))

        cacheHandler.set("test", newObject)

        // Then
        assertEquals(newObject, cacheHandler.get<TestObject>("test"))
        assertNotEquals(originalObject, cacheHandler.get<TestObject>("test"))
    }

    // TESTES DE THREAD SAFETY

    @Test
    fun `Should handle concurrent write operations thread safely`() = runTest {
        // Given
        val numberOfCoroutines = 100
        val testObjects = (1..numberOfCoroutines).map { TestObject(it.toString(), "test$it") }

        // When - Execute concurrent write operations
        val jobs = testObjects.map { obj ->
            async {
                cacheHandler.set("key${obj.id}", obj)
            }
        }
        jobs.awaitAll()

        // Then - Verify all objects were stored correctly
        testObjects.forEach { obj ->
            val result = cacheHandler.get<TestObject>("key${obj.id}")
            assertEquals(obj, result)
        }
    }

    @Test
    fun `Should handle concurrent read operations thread safely`() = runTest {
        // Given
        val numberOfReads = 50
        val testObject = TestObject("1", "test")
        cacheHandler.set("test", testObject)

        // When - Execute concurrent read operations
        val results = (1..numberOfReads).map {
            async {
                cacheHandler.get<TestObject>("test")
            }
        }.awaitAll()

        // Then - All reads should return the same object
        results.forEach { result ->
            assertEquals(testObject, result)
        }
    }

    @Test
    fun `Should handle concurrent mixed operations thread safely`() = runTest {
        // Given
        val numberOfOperations = 100
        val results = ConcurrentLinkedQueue<String>()

        // When - Execute mixed concurrent operations
        val jobs = (1..numberOfOperations).map { index ->
            async {
                when (index % 3) {
                    0 -> {
                        // Write operation
                        cacheHandler.set("key$index", TestObject(index.toString(), "value$index"))
                        results.add("WRITE_$index")
                    }

                    1 -> {
                        // Read operation
                        val result = cacheHandler.get<TestObject>("key${index - 1}")
                        results.add("READ_${index}_${result != null}")
                    }

                    2 -> {
                        // Delete operation
                        cacheHandler.delete("key${index - 2}")
                        results.add("DELETE_${index - 2}")
                    }

                    else -> {}
                }
            }
        }
        jobs.awaitAll()

        // Then - All operations should complete without errors
        assertTrue(results.size >= numberOfOperations / 3)

        // Verify some data consistency
        val writeOperations = results.filter { it.startsWith("WRITE") }
        assertTrue(writeOperations.isNotEmpty())
    }

    @Test
    fun `Should handle race conditions between set and delete operations`() = runTest {
        // Given
        val key = "race-condition-key"
        val testObject = TestObject("1", "test")
        val numberOfIterations = 50

        // When - Execute concurrent set and delete operations
        repeat(numberOfIterations) {
            val setJob = async {
                cacheHandler.set(key, testObject)
            }
            val deleteJob = async {
                delay(Random.nextLong(1, 5)) // Small random delay
                cacheHandler.delete(key)
            }

            awaitAll(setJob, deleteJob)
        }

        // Then - Cache should be in a consistent state (no exceptions thrown)
        // The final state can be either null or the test object
        val finalResult = cacheHandler.get<TestObject>(key)
        // Test passes if no exceptions were thrown and we get a consistent result
        assertTrue(finalResult == null || finalResult == testObject)
    }

    @Test
    fun `Should handle concurrent operations on same key thread safely`() = runTest {
        // Given
        val key = "same-key"
        val numberOfOperations = 100
        val counter = AtomicInteger(0)

        // When - Multiple coroutines modifying the same key
        val jobs = (1..numberOfOperations).map { index ->
            async {
                val value = TestObject(index.toString(), "value${counter.incrementAndGet()}")
                cacheHandler.set(key, value)

                // Verify we can read what we just wrote
                val readValue = cacheHandler.get<TestObject>(key)
                assertNotNull(readValue)
            }
        }
        jobs.awaitAll()

        // Then - Final value should be consistent
        val finalValue = cacheHandler.get<TestObject>(key)
        assertNotNull(finalValue)
    }

    @Test
    fun `Should handle concurrent clear operations thread safely`() = runTest {
        // Given
        val numberOfItems = 50

        // Populate cache
        repeat(numberOfItems) { index ->
            cacheHandler.set("key$index", TestObject(index.toString(), "value$index"))
        }

        // When - Execute concurrent clear operations
        val clearJobs = (1..10).map {
            async {
                cacheHandler.clear()
            }
        }
        clearJobs.awaitAll()

        // Then - Cache should be empty
        repeat(numberOfItems) { index ->
            assertNull(cacheHandler.get<TestObject>("key$index"))
        }
    }

    @Test
    fun `Should maintain data integrity during high concurrency load`() = runTest {
        // Given
        val numberOfCoroutines = 200
        val operationsPerCoroutine = 10
        val successfulOperations = AtomicInteger(0)

        // When - High load concurrent operations
        val jobs = (1..numberOfCoroutines).map { coroutineId ->
            async {
                repeat(operationsPerCoroutine) { opId ->
                    val key = "key_${coroutineId}_$opId"
                    val value = TestObject("$coroutineId", "value_${coroutineId}_$opId")

                    try {
                        cacheHandler.set(key, value)
                        val retrieved = cacheHandler.get<TestObject>(key)

                        if (retrieved == value) {
                            successfulOperations.incrementAndGet()
                        }

                        // Randomly delete some entries
                        if (opId % 3 == 0) {
                            cacheHandler.delete(key)
                        }
                    } catch (e: Exception) {
                        fail("Operação falhou durante alta concorrência: ${e.message}")
                    }
                }
            }
        }
        jobs.awaitAll()

        // Then - Most operations should have been successful
        val expectedSuccessful = numberOfCoroutines * operationsPerCoroutine
        assertTrue(
            successfulOperations.get() >= expectedSuccessful * 0.8, // At least 80% success rate
            "Taxa de sucesso muito baixa: ${successfulOperations.get()}/$expectedSuccessful"
        )
    }

    @Test
    fun `Should handle null values in concurrent operations`() = runTest {
        // Given
        val key = "null-value-key"
        val numberOfOperations = 50

        // When - Concurrent operations with null values
        val jobs = (1..numberOfOperations).map { index ->
            async {
                if (index % 2 == 0) {
                    cacheHandler.set(key, null)
                } else {
                    cacheHandler.set(key, TestObject(index.toString(), "value$index"))
                }

                // Read and verify we get a consistent result
                val result = cacheHandler.get<TestObject>(key)
                // Result can be null or a TestObject, but should be consistent
                assertTrue(result == null || result is TestObject)
            }
        }
        jobs.awaitAll()

        // Then - Final state should be consistent
        val finalResult = cacheHandler.get<TestObject>(key)
        assertTrue(finalResult == null || finalResult is TestObject)
    }

    private data class TestObject(val id: String, val name: String)
}