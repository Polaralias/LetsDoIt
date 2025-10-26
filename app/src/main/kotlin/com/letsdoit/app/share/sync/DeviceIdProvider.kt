package com.letsdoit.app.share.sync

import com.letsdoit.app.security.SecurePrefs
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface DeviceIdSource {
    fun deviceId(): String
}

@Singleton
class DeviceIdProvider @Inject constructor(private val securePrefs: SecurePrefs) : DeviceIdSource {
    private val key = "shared_lists_device_id"
    private val value: String by lazy {
        securePrefs.read(key) ?: UUID.randomUUID().toString().also { securePrefs.write(key, it) }
    }

    override fun deviceId(): String = value
}
