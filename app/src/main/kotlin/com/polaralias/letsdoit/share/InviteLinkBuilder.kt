package com.polaralias.letsdoit.share

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject

class InviteLinkBuilder @Inject constructor() {
    fun build(invite: ShareInvite): String {
        val base = StringBuilder("letsdoit://join")
        base.append("?shareId=").append(encode(invite.shareId))
        base.append("&transport=").append(invite.transport.name)
        base.append("&key=").append(encode(invite.key))
        invite.driveFolderId?.let {
            base.append("&driveFolderId=").append(encode(it))
        }
        return base.toString()
    }

    fun createNew(transport: ShareTransport, driveFolderId: String?): ShareInvite {
        val shareId = UUID.randomUUID().toString()
        val key = UUID.randomUUID().toString().replace("-", "")
        return ShareInvite(
            shareId = shareId,
            transport = transport,
            key = key,
            driveFolderId = driveFolderId,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
    }
}
