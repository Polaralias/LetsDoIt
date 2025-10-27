package com.polaralias.letsdoit.share.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.polaralias.letsdoit.data.db.AppDatabase
import com.polaralias.letsdoit.data.db.entities.ListEntity
import com.polaralias.letsdoit.data.db.entities.SpaceEntity
import com.polaralias.letsdoit.share.ShareTransport
import com.polaralias.letsdoit.share.crdt.ItemPayload
import com.polaralias.letsdoit.share.crdt.SharedListEventType
import com.polaralias.letsdoit.share.crdt.SharedListReducer
import com.polaralias.letsdoit.share.transport.DriveTransport
import com.polaralias.letsdoit.share.transport.LoopbackNearbyRouter
import com.polaralias.letsdoit.share.transport.LoopbackNearbyTransport
import com.squareup.moshi.Moshi
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

class SharedListSyncIntegrationTest {
    private lateinit var databaseA: AppDatabase
    private lateinit var databaseB: AppDatabase
    private val moshi = Moshi.Builder().build()
    private val reducer = SharedListReducer(moshi)
    private val itemAdapter = moshi.adapter(ItemPayload::class.java)
    private val router = LoopbackNearbyRouter()
    private val driveStub = object : DriveTransport {
        override suspend fun send(shareId: String, payload: ByteArray) = Unit
        override suspend fun receive(shareId: String): List<ByteArray> = emptyList()
    }

    @BeforeTest
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        databaseA = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        databaseB = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        initialiseLists(databaseA, "Alpha")
        initialiseLists(databaseB, "Beta")
    }

    @AfterTest
    fun tearDown() {
        databaseA.close()
        databaseB.close()
    }

    @Test
    fun loopbackTransportDeliversDuplicatesAndConverges() = runBlocking {
        val listIdA = databaseA.listDao().findByName("Alpha")!!.id
        val listIdB = databaseB.listDao().findByName("Beta")!!.id
        val transportA = LoopbackNearbyTransport(router, "device-a")
        val transportB = LoopbackNearbyTransport(router, "device-b")
        val managerA = SharedListSyncManager(
            databaseA.sharedListDao(),
            databaseA.crdtEventDao(),
            databaseA.eventAckDao(),
            reducer,
            moshi,
            driveStub,
            transportA,
            FakeDeviceIdSource("device-a"),
            FakeLamportCounter()
        )
        val managerB = SharedListSyncManager(
            databaseB.sharedListDao(),
            databaseB.crdtEventDao(),
            databaseB.eventAckDao(),
            reducer,
            moshi,
            driveStub,
            transportB,
            FakeDeviceIdSource("device-b"),
            FakeLamportCounter()
        )

        val shared = managerA.createShare(listIdA, ShareTransport.nearby)
        managerB.joinShare(listIdB, shared.shareId, shared.encKey, ShareTransport.nearby)

        managerA.recordEvent(listIdA, SharedListEventType.item, itemAdapter.toJson(ItemPayload(itemId = "1", value = "Milk")))
        managerA.recordEvent(listIdA, SharedListEventType.item, itemAdapter.toJson(ItemPayload(itemId = "2", value = "Bread")))

        managerA.sync(listIdA)
        managerA.sync(listIdA)

        managerB.sync(listIdB)

        managerB.recordEvent(listIdB, SharedListEventType.item, itemAdapter.toJson(ItemPayload(itemId = "1", value = "Milk - skim")))

        managerB.sync(listIdB)
        managerA.sync(listIdA)

        val stateA = managerA.materialise(listIdA).snapshot().items
        val stateB = managerB.materialise(listIdB).snapshot().items
        assertEquals(stateA, stateB)
        assertEquals(2, stateA.size)
        assertEquals("Milk - skim", stateA["1"]?.value)
    }

    private fun initialiseLists(database: AppDatabase, name: String) = runBlocking {
        val spaceDao = database.spaceDao()
        val listDao = database.listDao()
        spaceDao.upsert(SpaceEntity(name = "Sync"))
        val space = spaceDao.findByName("Sync") ?: throw IllegalStateException("Space missing")
        listDao.upsert(ListEntity(spaceId = space.id, name = name))
    }
}

private class FakeDeviceIdSource(private val id: String) : DeviceIdSource {
    override fun deviceId(): String = id
}

private class FakeLamportCounter : LamportCounter {
    private val values = mutableMapOf<Long, Long>()

    override fun next(listId: Long): Long {
        val current = values[listId] ?: 0L
        val next = current + 1
        values[listId] = next
        return next
    }

    override fun observe(listId: Long, lamport: Long) {
        val current = values[listId] ?: 0L
        if (lamport > current) {
            values[listId] = lamport
        }
    }
}
