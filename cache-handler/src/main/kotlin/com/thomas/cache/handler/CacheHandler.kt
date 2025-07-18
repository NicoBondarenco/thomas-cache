package com.thomas.cache.handler

interface CacheHandler {

    suspend fun <T : Any> set(key: String, value: T?)

    suspend fun <T : Any> get(key: String): T?

    suspend fun delete(key: String)

    suspend fun clear()

}