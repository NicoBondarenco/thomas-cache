package com.thomas.cache.handler

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SimpleCacheHandler(
    private val cache: MutableMap<String, Any?> = HashMap()
) : CacheHandler {

    private val mutex = Mutex()

    override suspend fun <T : Any> set(key: String, value: T?) {
        mutex.withLock {
            cache[key] = value
        }
    }

    override suspend fun <T : Any> get(key: String): T? {
        return mutex.withLock {
            cache[key] as T?
        }
    }

    override suspend fun delete(key: String) {
        mutex.withLock {
            cache.remove(key)
        }
    }

    override suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }
}