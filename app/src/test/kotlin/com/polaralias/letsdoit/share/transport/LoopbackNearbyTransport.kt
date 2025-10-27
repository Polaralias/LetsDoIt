package com.polaralias.letsdoit.share.transport

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class LoopbackNearbyRouter {
    private val mutex = Mutex()
    private val storage = mutableMapOf<String, MutableList<Pair<String, ByteArray>>>()

    suspend fun publish(senderId: String, shareId: String, payload: ByteArray) {
        mutex.withLock {
            val queue = storage.getOrPut(shareId) { mutableListOf() }
            queue.add(senderId to payload.copyOf())
        }
    }

    suspend fun collect(receiverId: String, shareId: String): List<ByteArray> {
        return mutex.withLock {
            val queue = storage[shareId] ?: return@withLock emptyList()
            val (deliver, keep) = queue.partition { it.first != receiverId }
            storage[shareId] = keep.toMutableList()
            deliver.map { it.second.copyOf() }
        }
    }
}

class LoopbackNearbyTransport(
    private val router: LoopbackNearbyRouter,
    private val deviceId: String
) : NearbyTransport {
    override suspend fun send(shareId: String, payload: ByteArray) {
        router.publish(deviceId, shareId, payload)
    }

    override suspend fun receive(shareId: String): List<ByteArray> {
        return router.collect(deviceId, shareId)
    }
}
