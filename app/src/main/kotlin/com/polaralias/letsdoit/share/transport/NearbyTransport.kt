package com.polaralias.letsdoit.share.transport

interface NearbyTransport {
    suspend fun send(shareId: String, payload: ByteArray)

    suspend fun receive(shareId: String): List<ByteArray>
}
