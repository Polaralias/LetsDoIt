package com.polaralias.letsdoit.data.remote.service

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.polaralias.letsdoit.data.worker.SyncScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FcmService : FirebaseMessagingService() {

    @Inject
    lateinit var syncScheduler: SyncScheduler

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // In a real app, send this token to backend.
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Check if message contains data payload.
        if (remoteMessage.data.isNotEmpty()) {
            val taskId = remoteMessage.data["task_id"]
            syncScheduler.scheduleOneTimeSync(taskId)
        }
    }
}
