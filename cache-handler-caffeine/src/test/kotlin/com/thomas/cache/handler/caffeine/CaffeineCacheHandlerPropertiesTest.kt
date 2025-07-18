package com.thomas.cache.handler.caffeine

import java.util.stream.Stream
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource

class CaffeineCacheHandlerPropertiesTest {

    companion object {
        private const val VALID_INITIAL_CAPACITY = 1000
        private const val VALID_MAXIMUM_SIZE = 5000
        private const val VALID_EXPIRE_WRITE_SECONDS = 3600L
        private const val VALID_EXPIRE_ACCESS_SECONDS = 1800L

        @JvmStatic
        fun validPropertiesProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(1, 1, 0, 1L, 1L),
                Arguments.of(1000, 5000, 50, 3600L, 1800L),
                Arguments.of(10000, 10000, 100, 86400L, 43200L),
                Arguments.of(500, 2000, 25, 7200L, 3600L),
                Arguments.of(Int.MAX_VALUE - 1, Int.MAX_VALUE, 0, Long.MAX_VALUE, Long.MAX_VALUE)
            )
        }

        @JvmStatic
        fun invalidPropertiesProvider(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(0, 1000, 3600L, 1800L, "Initial cache capacity must be greater than 0"),
                Arguments.of(-1, 1000, 3600L, 1800L, "Initial cache capacity must be greater than 0"),
                Arguments.of(1000, 0, 3600L, 1800L, "Initial cache capacity must be greater than 0"),
                Arguments.of(1000, -1, 3600L, 1800L, "Initial cache capacity must be greater than 0"),
                Arguments.of(5000, 1000, 3600L, 1800L, "Maximum cache size must be greater than or equal to initial cache capacity"),
                Arguments.of(1000, 5000, 0L, 1800L, "Expire write seconds must be greater than 0"),
                Arguments.of(1000, 5000, -1L, 1800L, "Expire write seconds must be greater than 0"),
                Arguments.of(1000, 5000, 3600L, 0L, "Expire access seconds must be greater than 0"),
                Arguments.of(1000, 5000, 3600L, -1L, "Expire access seconds must be greater than 0")
            )
        }

    }

    @Test
    fun `Should create instance with valid parameters`() {
        // Given & When
        val properties = CaffeineCacheHandlerProperties(
            initialCacheCapacity = VALID_INITIAL_CAPACITY,
            maximumCacheSize = VALID_MAXIMUM_SIZE,
            expireWriteSeconds = VALID_EXPIRE_WRITE_SECONDS,
            expireAccessSeconds = VALID_EXPIRE_ACCESS_SECONDS
        )

        // Then
        assertEquals(VALID_INITIAL_CAPACITY, properties.initialCacheCapacity)
        assertEquals(VALID_MAXIMUM_SIZE, properties.maximumCacheSize)
        assertEquals(VALID_EXPIRE_WRITE_SECONDS, properties.expireWriteSeconds)
        assertEquals(VALID_EXPIRE_ACCESS_SECONDS, properties.expireAccessSeconds)
    }

    @Test
    fun `Should create instance with minimum valid values`() {
        // Given & When
        val properties = CaffeineCacheHandlerProperties(
            initialCacheCapacity = 1,
            maximumCacheSize = 1,
            expireWriteSeconds = 1L,
            expireAccessSeconds = 1L
        )

        // Then
        assertEquals(1, properties.initialCacheCapacity)
        assertEquals(1, properties.maximumCacheSize)
        assertEquals(1L, properties.expireWriteSeconds)
        assertEquals(1L, properties.expireAccessSeconds)
    }

    @Test
    fun `Should create instance when maximum size equals initial capacity`() {
        // Given & When
        val capacity = 1000
        val properties = CaffeineCacheHandlerProperties(
            initialCacheCapacity = capacity,
            maximumCacheSize = capacity,
            expireWriteSeconds = VALID_EXPIRE_WRITE_SECONDS,
            expireAccessSeconds = VALID_EXPIRE_ACCESS_SECONDS
        )

        // Then
        assertEquals(capacity, properties.initialCacheCapacity)
        assertEquals(capacity, properties.maximumCacheSize)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1, -10, -100, Int.MIN_VALUE])
    fun `Should throw exception when initial cache capacity is invalid`(invalidCapacity: Int) {
        // Given & When & Then
        val exception = assertThrows<IllegalArgumentException> {
            CaffeineCacheHandlerProperties(
                initialCacheCapacity = invalidCapacity,
                maximumCacheSize = VALID_MAXIMUM_SIZE,
                expireWriteSeconds = VALID_EXPIRE_WRITE_SECONDS,
                expireAccessSeconds = VALID_EXPIRE_ACCESS_SECONDS
            )
        }

        assertTrue(exception.message!!.contains("Initial cache capacity must be greater than 0"))
    }

    @ParameterizedTest
    @ValueSource(ints = [0, -1, -10, -100, Int.MIN_VALUE])
    fun `Should throw exception when maximum cache size is invalid`(invalidMaxSize: Int) {
        // Given & When & Then
        val exception = assertThrows<IllegalArgumentException> {
            CaffeineCacheHandlerProperties(
                initialCacheCapacity = VALID_INITIAL_CAPACITY,
                maximumCacheSize = invalidMaxSize,
                expireWriteSeconds = VALID_EXPIRE_WRITE_SECONDS,
                expireAccessSeconds = VALID_EXPIRE_ACCESS_SECONDS
            )
        }

        assertTrue(exception.message!!.contains("Initial cache capacity must be greater than 0"))
    }

    @Test
    fun `Should throw exception when maximum cache size is less than initial capacity`() {
        // Given & When & Then
        val exception = assertThrows<IllegalArgumentException> {
            CaffeineCacheHandlerProperties(
                initialCacheCapacity = 5000,
                maximumCacheSize = 1000,
                expireWriteSeconds = VALID_EXPIRE_WRITE_SECONDS,
                expireAccessSeconds = VALID_EXPIRE_ACCESS_SECONDS
            )
        }

        assertTrue(exception.message!!.contains("Maximum cache size must be greater than or equal to initial cache capacity"))
    }

    @ParameterizedTest
    @ValueSource(longs = [0L, -1L, -10L, -100L, Long.MIN_VALUE])
    fun `Should throw exception when expire write seconds is invalid`(invalidExpireWrite: Long) {
        // Given & When & Then
        val exception = assertThrows<IllegalArgumentException> {
            CaffeineCacheHandlerProperties(
                initialCacheCapacity = VALID_INITIAL_CAPACITY,
                maximumCacheSize = VALID_MAXIMUM_SIZE,
                expireWriteSeconds = invalidExpireWrite,
                expireAccessSeconds = VALID_EXPIRE_ACCESS_SECONDS
            )
        }

        assertTrue(exception.message!!.contains("Expire write seconds must be greater than 0"))
    }

    @ParameterizedTest
    @ValueSource(longs = [0L, -1L, -10L, -100L, Long.MIN_VALUE])
    fun `Should throw exception when expire access seconds is invalid`(invalidExpireAccess: Long) {
        // Given & When & Then
        val exception = assertThrows<IllegalArgumentException> {
            CaffeineCacheHandlerProperties(
                initialCacheCapacity = VALID_INITIAL_CAPACITY,
                maximumCacheSize = VALID_MAXIMUM_SIZE,
                expireWriteSeconds = VALID_EXPIRE_WRITE_SECONDS,
                expireAccessSeconds = invalidExpireAccess
            )
        }

        assertTrue(exception.message!!.contains("Expire access seconds must be greater than 0"))
    }

    @Test
    fun `Should throw exception with multiple validation errors`() {
        // Given & When & Then
        val exception = assertThrows<IllegalArgumentException> {
            CaffeineCacheHandlerProperties(
                initialCacheCapacity = -1,
                maximumCacheSize = -1,
                expireWriteSeconds = -1L,
                expireAccessSeconds = -1L
            )
        }

        val message = exception.message!!
        assertTrue(message.contains("Initial cache capacity must be greater than 0"))
        assertTrue(message.contains("Expire write seconds must be greater than 0"))
        assertTrue(message.contains("Expire access seconds must be greater than 0"))
    }

    @Test
    fun `Should throw exception when initial capacity is greater than maximum size and other errors exist`() {
        // Given & When & Then
        val exception = assertThrows<IllegalArgumentException> {
            CaffeineCacheHandlerProperties(
                initialCacheCapacity = 5000,
                maximumCacheSize = 1000,
                expireWriteSeconds = VALID_EXPIRE_WRITE_SECONDS,
                expireAccessSeconds = VALID_EXPIRE_ACCESS_SECONDS
            )
        }

        val message = exception.message!!
        assertTrue(message.contains("Maximum cache size must be greater than or equal to initial cache capacity"))
    }

    @ParameterizedTest
    @MethodSource("validPropertiesProvider")
    fun `Should create instance with various valid combinations`(
        initialCapacity: Int,
        maximumSize: Int,
        bufferPercentage: Int,
        expireWrite: Long,
        expireAccess: Long
    ) {
        // Given & When
        val properties = CaffeineCacheHandlerProperties(
            initialCacheCapacity = initialCapacity,
            maximumCacheSize = maximumSize,
            expireWriteSeconds = expireWrite,
            expireAccessSeconds = expireAccess
        )

        // Then
        assertEquals(initialCapacity, properties.initialCacheCapacity)
        assertEquals(maximumSize, properties.maximumCacheSize)
        assertEquals(expireWrite, properties.expireWriteSeconds)
        assertEquals(expireAccess, properties.expireAccessSeconds)
    }

    @ParameterizedTest
    @MethodSource("invalidPropertiesProvider")
    fun `Should throw exception with various invalid combinations`(
        initialCapacity: Int,
        maximumSize: Int,
        expireWrite: Long,
        expireAccess: Long,
        expectedErrorFragment: String
    ) {
        // Given & When & Then
        val exception = assertThrows<IllegalArgumentException> {
            CaffeineCacheHandlerProperties(
                initialCacheCapacity = initialCapacity,
                maximumCacheSize = maximumSize,
                expireWriteSeconds = expireWrite,
                expireAccessSeconds = expireAccess
            )
        }

        assertTrue(
            exception.message!!.contains(expectedErrorFragment),
            "Expected error message to contain '$expectedErrorFragment' but was: ${exception.message}"
        )
    }

    @Test
    fun `Should handle maximum integer values correctly`() {
        // Given & When
        val properties = CaffeineCacheHandlerProperties(
            initialCacheCapacity = Int.MAX_VALUE - 1,
            maximumCacheSize = Int.MAX_VALUE,
            expireWriteSeconds = Long.MAX_VALUE,
            expireAccessSeconds = Long.MAX_VALUE
        )

        // Then
        assertEquals(Int.MAX_VALUE - 1, properties.initialCacheCapacity)
        assertEquals(Int.MAX_VALUE, properties.maximumCacheSize)
        assertEquals(Long.MAX_VALUE, properties.expireWriteSeconds)
        assertEquals(Long.MAX_VALUE, properties.expireAccessSeconds)
    }

    @Test
    fun `Should implement data class methods correctly`() {
        // Given
        val properties1 = CaffeineCacheHandlerProperties(
            initialCacheCapacity = VALID_INITIAL_CAPACITY,
            maximumCacheSize = VALID_MAXIMUM_SIZE,
            expireWriteSeconds = VALID_EXPIRE_WRITE_SECONDS,
            expireAccessSeconds = VALID_EXPIRE_ACCESS_SECONDS
        )

        val properties2 = CaffeineCacheHandlerProperties(
            initialCacheCapacity = VALID_INITIAL_CAPACITY,
            maximumCacheSize = VALID_MAXIMUM_SIZE,
            expireWriteSeconds = VALID_EXPIRE_WRITE_SECONDS,
            expireAccessSeconds = VALID_EXPIRE_ACCESS_SECONDS
        )

        val properties3 = CaffeineCacheHandlerProperties(
            initialCacheCapacity = VALID_INITIAL_CAPACITY + 1, // Diferente
            maximumCacheSize = VALID_MAXIMUM_SIZE,
            expireWriteSeconds = VALID_EXPIRE_WRITE_SECONDS,
            expireAccessSeconds = VALID_EXPIRE_ACCESS_SECONDS
        )

        // Then
        // Equals
        assertEquals(properties1, properties2)
        assertNotEquals(properties1, properties3)

        // HashCode
        assertEquals(properties1.hashCode(), properties2.hashCode())
        assertNotEquals(properties1.hashCode(), properties3.hashCode())

        // ToString
        val toString = properties1.toString()
        assertTrue(toString.contains("CaffeineCacheHandlerProperties"))
        assertTrue(toString.contains("initialCacheCapacity=$VALID_INITIAL_CAPACITY"))
        assertTrue(toString.contains("maximumCacheSize=$VALID_MAXIMUM_SIZE"))
    }

    @Test
    fun `Should create copy with different values`() {
        // Given
        val original = CaffeineCacheHandlerProperties(
            initialCacheCapacity = VALID_INITIAL_CAPACITY,
            maximumCacheSize = VALID_MAXIMUM_SIZE,
            expireWriteSeconds = VALID_EXPIRE_WRITE_SECONDS,
            expireAccessSeconds = VALID_EXPIRE_ACCESS_SECONDS
        )

        // When
        val copied = original.copy(
            initialCacheCapacity = VALID_INITIAL_CAPACITY + 100,
            expireWriteSeconds = VALID_EXPIRE_WRITE_SECONDS + 100
        )

        // Then
        assertEquals(VALID_INITIAL_CAPACITY + 100, copied.initialCacheCapacity)
        assertEquals(VALID_MAXIMUM_SIZE, copied.maximumCacheSize)
        assertEquals(VALID_EXPIRE_WRITE_SECONDS + 100, copied.expireWriteSeconds)
        assertEquals(VALID_EXPIRE_ACCESS_SECONDS, copied.expireAccessSeconds)
    }

}