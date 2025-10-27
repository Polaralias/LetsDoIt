package com.polaralias.letsdoit.share.sync

import com.polaralias.letsdoit.security.SecurePrefs
import javax.inject.Inject
import javax.inject.Singleton

interface LamportCounter {
    fun next(listId: Long): Long
    fun observe(listId: Long, lamport: Long)
}

@Singleton
class LamportClock @Inject constructor(private val securePrefs: SecurePrefs) : LamportCounter {
    override fun next(listId: Long): Long {
        val key = key(listId)
        val current = securePrefs.read(key)?.toLongOrNull() ?: 0L
        val value = current + 1
        securePrefs.write(key, value.toString())
        return value
    }

    override fun observe(listId: Long, lamport: Long) {
        val key = key(listId)
        val current = securePrefs.read(key)?.toLongOrNull() ?: 0L
        if (lamport > current) {
            securePrefs.write(key, lamport.toString())
        }
    }

    private fun key(listId: Long): String = "shared_lists_lamport_$listId"
}
