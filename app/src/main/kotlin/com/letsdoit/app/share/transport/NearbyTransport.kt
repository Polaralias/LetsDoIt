package com.letsdoit.app.share.transport

interface NearbyTransport {
    suspend fun send(shareId: String, payload: ByteArray)

    suspend fun receive(shareId: String): List<ByteArray>
}
