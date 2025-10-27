package com.polaralias.letsdoit.share.crdt

import com.polaralias.letsdoit.data.db.entities.CrdtEventEntity
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import javax.inject.Inject

@JsonClass(generateAdapter = true)
data class ItemPayload(
    val itemId: String,
    val value: String?,
    val deleted: Boolean = false
)

data class VersionedItem(
    val itemId: String,
    val value: String?,
    val deleted: Boolean,
    val lamport: Long,
    val authorDeviceId: String,
    val timestamp: Long,
    val eventId: String
)

class SharedListMaterialisedState internal constructor(
    internal val mutableItems: MutableMap<String, VersionedItem>,
    internal val appliedIdSet: MutableSet<String>
) {
    constructor() : this(mutableMapOf(), mutableSetOf())

    val items: Map<String, VersionedItem> get() = mutableItems
    val appliedIds: Set<String> get() = appliedIdSet

    fun snapshot(): SharedListSnapshot = SharedListSnapshot(mutableItems.toMap())
}

data class SharedListSnapshot(val items: Map<String, VersionedItem>)

object SharedListEventType {
    const val item = "item"
}

class SharedListReducer @Inject constructor(moshi: Moshi) {
    private val itemAdapter = moshi.adapter(ItemPayload::class.java)

    fun apply(state: SharedListMaterialisedState, events: List<CrdtEventEntity>): SharedListMaterialisedState {
        val next = SharedListMaterialisedState(state.mutableItems.toMutableMap(), state.appliedIdSet.toMutableSet())
        val sorted = events.sortedWith(compareBy<CrdtEventEntity>({ it.lamport }, { it.authorDeviceId }, { it.id }))
        for (event in sorted) {
            if (!next.appliedIdSet.add(event.id)) continue
            when (event.type) {
                SharedListEventType.item -> applyItemEvent(next, event)
            }
        }
        return next
    }

    private fun applyItemEvent(state: SharedListMaterialisedState, event: CrdtEventEntity) {
        val payload = itemAdapter.fromJson(event.payloadJson) ?: return
        val existing = state.mutableItems[payload.itemId]
        if (existing != null) {
            if (!outranks(event, existing)) return
        }
        val deleted = payload.deleted || payload.value == null
        val versioned = VersionedItem(
            itemId = payload.itemId,
            value = payload.value,
            deleted = deleted,
            lamport = event.lamport,
            authorDeviceId = event.authorDeviceId,
            timestamp = event.timestamp,
            eventId = event.id
        )
        if (deleted) {
            state.mutableItems.remove(payload.itemId)
        } else {
            state.mutableItems[payload.itemId] = versioned
        }
    }

    private fun outranks(event: CrdtEventEntity, current: VersionedItem): Boolean {
        if (event.lamport > current.lamport) return true
        if (event.lamport < current.lamport) return false
        val comparison = event.authorDeviceId.compareTo(current.authorDeviceId)
        if (comparison > 0) return true
        if (comparison < 0) return false
        return event.id > current.eventId
    }
}
