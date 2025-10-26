package com.letsdoit.app.share.crdt

import com.letsdoit.app.data.db.entities.CrdtEventEntity
import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedListReducerTest {
    private val moshi = Moshi.Builder().build()
    private val reducer = SharedListReducer(moshi)
    private val adapter = moshi.adapter(ItemPayload::class.java)

    @Test
    fun appliesEventsIdempotently() {
        val state = SharedListMaterialisedState()
        val event = event(
            id = "evt-1",
            lamport = 1,
            author = "device-a",
            value = "Apples"
        )
        val first = reducer.apply(state, listOf(event, event))
        assertEquals(1, first.items.size)
        assertEquals("Apples", first.items["item-1"]?.value)

        val second = reducer.apply(first, listOf(event))
        assertEquals(1, second.items.size)
        assertEquals("Apples", second.items["item-1"]?.value)
    }

    @Test
    fun resolvesConflictsByLamportThenAuthor() {
        val state = SharedListMaterialisedState()
        val lowerLamport = event(
            id = "evt-1",
            lamport = 1,
            author = "device-b",
            value = "Bread"
        )
        val higherLamport = event(
            id = "evt-2",
            lamport = 2,
            author = "device-a",
            value = "Butter"
        )
        val sameLamportLowerAuthor = event(
            id = "evt-3",
            lamport = 2,
            author = "device-a",
            value = "Biscuits"
        )
        val sameLamportHigherAuthor = event(
            id = "evt-4",
            lamport = 2,
            author = "device-z",
            value = "Batter"
        )

        val afterFirst = reducer.apply(state, listOf(lowerLamport))
        assertEquals("Bread", afterFirst.items["item-1"]?.value)
        val afterSecond = reducer.apply(afterFirst, listOf(sameLamportLowerAuthor))
        assertEquals("Biscuits", afterSecond.items["item-1"]?.value)
        val afterThird = reducer.apply(afterSecond, listOf(higherLamport))
        assertEquals("Butter", afterThird.items["item-1"]?.value)
        val afterFourth = reducer.apply(afterThird, listOf(sameLamportHigherAuthor))
        assertEquals("Batter", afterFourth.items["item-1"]?.value)
        assertTrue(afterFourth.appliedIds.containsAll(listOf("evt-1", "evt-2", "evt-3", "evt-4")))
    }

    private fun event(id: String, lamport: Long, author: String, value: String): CrdtEventEntity {
        val payload = ItemPayload(itemId = "item-1", value = value)
        return CrdtEventEntity(
            id = id,
            listId = 1,
            authorDeviceId = author,
            lamport = lamport,
            timestamp = lamport * 1000,
            type = SharedListEventType.item,
            payloadJson = adapter.toJson(payload),
            applied = false
        )
    }
}
