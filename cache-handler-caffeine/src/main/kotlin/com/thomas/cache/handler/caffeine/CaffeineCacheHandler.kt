package com.thomas.cache.handler.caffeine

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.Scheduler
import com.thomas.cache.handler.CacheHandler
import dev.hsbrysk.caffeine.CoroutineCache
import dev.hsbrysk.caffeine.buildCoroutine
import java.time.Duration
import java.util.Optional
import java.util.concurrent.Executors

class CaffeineCacheHandler(
    properties: CaffeineCacheHandlerProperties
) : CacheHandler {

    private val factory = Thread.ofVirtual().factory()
    private val service = Executors.newScheduledThreadPool(0, factory)
    private val scheduler: Scheduler = Scheduler.forScheduledExecutorService(service)

    private val coroutineCache: CoroutineCache<String, Optional<Any>> = Caffeine.newBuilder()
        .initialCapacity(properties.initialCacheCapacity)
        .maximumSize(properties.maximumCacheSize.toLong())
        .expireAfterWrite(Duration.ofSeconds(properties.expireWriteSeconds))
        .expireAfterAccess(Duration.ofSeconds(properties.expireAccessSeconds))
        .scheduler(scheduler)
        .buildCoroutine()


    override suspend fun <T : Any> set(key: String, value: T?) {
        coroutineCache.put(key, Optional.ofNullable(value))
    }

    override suspend fun <T : Any> get(key: String): T? =
        coroutineCache.getIfPresent(key)?.orElse(null) as? T?

    override suspend fun delete(key: String) {
        coroutineCache.synchronous().invalidate(key)
    }

    override suspend fun clear() {
        coroutineCache.synchronous().invalidateAll()
    }

}

