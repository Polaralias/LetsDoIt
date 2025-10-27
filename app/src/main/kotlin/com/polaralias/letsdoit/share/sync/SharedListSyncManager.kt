package com.polaralias.letsdoit.share.sync

import com.polaralias.letsdoit.data.db.dao.CrdtEventDao
import com.polaralias.letsdoit.data.db.dao.EventAckDao
import com.polaralias.letsdoit.data.db.dao.SharedListDao
import com.polaralias.letsdoit.data.db.entities.CrdtEventEntity
import com.polaralias.letsdoit.data.db.entities.EventAckEntity
import com.polaralias.letsdoit.data.db.entities.SharedListEntity
import com.polaralias.letsdoit.share.ShareTransport
import com.polaralias.letsdoit.share.crdt.SharedListMaterialisedState
import com.polaralias.letsdoit.share.crdt.SharedListReducer
import com.polaralias.letsdoit.share.crypto.LogpackCrypto
import com.polaralias.letsdoit.share.transport.DriveTransport
import com.polaralias.letsdoit.share.transport.NearbyTransport
import com.squareup.moshi.Moshi
import java.nio.charset.Charset
import java.security.SecureRandom
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedListSyncManager @Inject constructor(
    private val sharedListDao: SharedListDao,
    private val crdtEventDao: CrdtEventDao,
    private val eventAckDao: EventAckDao,
    private val reducer: SharedListReducer,
    moshi: Moshi,
    private val driveTransport: DriveTransport,
    private val nearbyTransport: NearbyTransport,
    private val deviceIdProvider: DeviceIdSource,
    private val lamportClock: LamportCounter
) {
    private val logpackAdapter = moshi.adapter(Logpack::class.java)
    private val secureRandom = SecureRandom()
    private val charset: Charset = Charsets.UTF_8

    suspend fun createShare(listId: Long, transport: ShareTransport): SharedListEntity {
        val existing = sharedListDao.getByListId(listId)
        if (existing != null) return existing
        val key = ByteArray(32).also { secureRandom.nextBytes(it) }
        val entity = SharedListEntity(
            listId = listId,
            shareId = UUID.randomUUID().toString(),
            encKey = key,
            transport = transport.name,
            createdAt = System.currentTimeMillis()
        )
        sharedListDao.upsert(entity)
        return entity
    }

    suspend fun joinShare(listId: Long, shareId: String, key: ByteArray, transport: ShareTransport): SharedListEntity {
        val existing = sharedListDao.getByListId(listId)
        if (existing != null) return existing
        val entity = SharedListEntity(
            listId = listId,
            shareId = shareId,
            encKey = key,
            transport = transport.name,
            createdAt = System.currentTimeMillis()
        )
        sharedListDao.upsert(entity)
        return entity
    }

    suspend fun recordEvent(listId: Long, type: String, payloadJson: String): CrdtEventEntity {
        val lamport = lamportClock.next(listId)
        val event = CrdtEventEntity(
            id = UUID.randomUUID().toString(),
            listId = listId,
            authorDeviceId = deviceIdProvider.deviceId(),
            lamport = lamport,
            timestamp = System.currentTimeMillis(),
            type = type,
            payloadJson = payloadJson,
            applied = true
        )
        val inserted = crdtEventDao.insert(event)
        return if (inserted == -1L) {
            crdtEventDao.findById(event.id) ?: event
        } else {
            event
        }
    }

    suspend fun materialise(listId: Long): SharedListMaterialisedState {
        val events = crdtEventDao.listForList(listId)
        val state = reducer.apply(SharedListMaterialisedState(), events)
        for (event in events) {
            if (!event.applied) {
                crdtEventDao.markApplied(event.id)
            }
        }
        return state
    }

    suspend fun reset() {
        eventAckDao.deleteAll()
        crdtEventDao.deleteAll()
        sharedListDao.deleteAll()
    }

    suspend fun sync(listId: Long) {
        val shared = sharedListDao.getByListId(listId) ?: return
        materialise(listId)
        val deviceId = deviceIdProvider.deviceId()
        val eventsToSend = crdtEventDao.pendingForRemote(listId)
        val acknowledgements = crdtEventDao.acknowledgements(listId, deviceId)
        if (eventsToSend.isNotEmpty() || acknowledgements.isNotEmpty()) {
            val logpack = Logpack(
                events = eventsToSend.map { it.toLogpackEvent() },
                acknowledgements = acknowledgements
            )
            val payload = logpackAdapter.toJson(logpack).toByteArray(charset)
            val encrypted = LogpackCrypto.encrypt(shared.encKey, payload)
            sendPayload(shared, encrypted)
        }
        val packets = receivePackets(shared)
        if (packets.isEmpty()) return
        var hasNewEvents = false
        for (packet in packets) {
            val decrypted = runCatching { LogpackCrypto.decrypt(shared.encKey, packet) }.getOrNull() ?: continue
            val logpack = runCatching { logpackAdapter.fromJson(String(decrypted, charset)) }.getOrNull() ?: continue
            for (ack in logpack.acknowledgements) {
                eventAckDao.upsert(EventAckEntity(ack, listId, System.currentTimeMillis()))
            }
            for (event in logpack.events) {
                lamportClock.observe(listId, event.lamport)
                val entity = CrdtEventEntity(
                    id = event.id,
                    listId = listId,
                    authorDeviceId = event.authorDeviceId,
                    lamport = event.lamport,
                    timestamp = event.timestamp,
                    type = event.type,
                    payloadJson = event.payloadJson,
                    applied = false
                )
                val inserted = crdtEventDao.insert(entity)
                if (inserted == -1L) {
                    val existing = crdtEventDao.findById(event.id)
                    if (existing != null && !existing.applied) {
                        hasNewEvents = true
                    }
                } else {
                    hasNewEvents = true
                }
            }
        }
        if (hasNewEvents) {
            materialise(listId)
        }
    }

    private suspend fun sendPayload(sharedList: SharedListEntity, payload: ByteArray) {
        when (ShareTransport.valueOf(sharedList.transport)) {
            ShareTransport.drive -> driveTransport.send(sharedList.shareId, payload)
            ShareTransport.nearby -> nearbyTransport.send(sharedList.shareId, payload)
        }
    }

    private suspend fun receivePackets(sharedList: SharedListEntity): List<ByteArray> {
        return when (ShareTransport.valueOf(sharedList.transport)) {
            ShareTransport.drive -> driveTransport.receive(sharedList.shareId)
            ShareTransport.nearby -> nearbyTransport.receive(sharedList.shareId)
        }
    }

    private fun CrdtEventEntity.toLogpackEvent(): LogpackEvent {
        return LogpackEvent(
            id = id,
            authorDeviceId = authorDeviceId,
            lamport = lamport,
            timestamp = timestamp,
            type = type,
            payloadJson = payloadJson
        )
    }
}
