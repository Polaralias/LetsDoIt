package com.polaralias.letsdoit.share

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import com.polaralias.letsdoit.security.SecurePrefs
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import java.io.File

class InviteLinkTest {
    private val builder = InviteLinkBuilder()
    private val parser = InviteLinkParser()

    @Test
    fun builderAndParserRoundTrip() {
        val invite = ShareInvite(
            shareId = "share-id",
            transport = ShareTransport.drive,
            key = "secret-key",
            driveFolderId = "folder-123",
            createdAt = 1234L
        )
        val link = builder.build(invite)
        val parsed = parser.parse(link)
        assertEquals(invite.shareId, parsed.shareId)
        assertEquals(invite.transport, parsed.transport)
        assertEquals(invite.key, parsed.key)
        assertEquals(invite.driveFolderId, parsed.driveFolderId)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun nearbyTogglePersists() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.filesDir, "share_prefs_test.preferences_pb")
        if (file.exists()) file.delete()
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            file
        }
        val securePrefs = SecurePrefs(context)
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val repository = ShareRepository(dataStore, securePrefs, moshi)
        val initial = repository.readNearbyState()
        assertTrue(!initial.discoverable)
        val updatedState = initial.copy(discoverable = true, deviceName = "Test Device")
        repository.updateNearby(updatedState)
        val stored = repository.readNearbyState()
        assertTrue(stored.discoverable)
        assertEquals("Test Device", stored.deviceName)
        val flowState = repository.shareState.first { it.nearby.discoverable }
        assertEquals("Test Device", flowState.nearby.deviceName)
    }
}
