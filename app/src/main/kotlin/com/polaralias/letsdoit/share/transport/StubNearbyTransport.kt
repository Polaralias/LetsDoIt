package com.polaralias.letsdoit.share.transport

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class StubNearbyTransport @Inject constructor() : NearbyTransport {
    private val mutex = Mutex()
    private val storage = mutableMapOf<String, MutableList<ByteArray>>()

    override suspend fun send(shareId: String, payload: ByteArray) {
        mutex.withLock {
            val queue = storage.getOrPut(shareId) { mutableListOf() }
            queue.add(payload.copyOf())
        }
    }

    override suspend fun receive(shareId: String): List<ByteArray> {
        return mutex.withLock {
            storage[shareId]?.map { it.copyOf() } ?: emptyList()
        }
    }

    suspend fun clear(shareId: String) {
        mutex.withLock {
            storage.remove(shareId)
        }
    }
}
