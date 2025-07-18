package com.thomas.cache.handler.caffeine

data class CaffeineCacheHandlerProperties(
    val initialCacheCapacity: Int,
    val maximumCacheSize: Int,
    val expireWriteSeconds: Long,
    val expireAccessSeconds: Long,
) {

    init {
        validate()
    }

    private fun validate() {
        listOf(
            validateInitialCacheCapacity(),
            validateMaximumCacheSize(),
            validateExpireWriteSeconds(),
            validateExpireAccessSeconds(),
        ).flatten().takeIf { it.isNotEmpty() }?.let {
            throw IllegalArgumentException(it.joinToString(", "))
        }
    }

    private fun validateInitialCacheCapacity(): List<String> {
        val list: MutableList<String> = mutableListOf()
        if (initialCacheCapacity <= 0) {
            list.add("Initial cache capacity must be greater than 0")
        }
        return list
    }

    private fun validateMaximumCacheSize(): List<String> {
        val list: MutableList<String> = mutableListOf()
        if (maximumCacheSize <= 0) {
            list.add("Initial cache capacity must be greater than 0")
        }
        if (maximumCacheSize < initialCacheCapacity) {
            list.add("Maximum cache size must be greater than or equal to initial cache capacity")
        }
        return list
    }

    private fun validateExpireWriteSeconds(): List<String> {
        val list: MutableList<String> = mutableListOf()
        if (expireWriteSeconds <= 0) {
            list.add("Expire write seconds must be greater than 0")
        }
        return list
    }

    private fun validateExpireAccessSeconds(): List<String> {
        val list: MutableList<String> = mutableListOf()
        if (expireAccessSeconds <= 0) {
            list.add("Expire access seconds must be greater than 0")
        }
        return list
    }

}