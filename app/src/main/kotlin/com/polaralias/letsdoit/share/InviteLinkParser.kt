package com.polaralias.letsdoit.share

import android.net.Uri
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

class InviteLinkParser @Inject constructor() {
    fun parse(link: String): ShareInvite {
        val uri = Uri.parse(link)
        val scheme = uri.scheme ?: error("Invalid scheme")
        require(scheme == "letsdoit") { "Unsupported scheme" }
        val shareId = uri.getQueryParameter("shareId")?.let { decode(it) } ?: error("Missing shareId")
        val transportParam = uri.getQueryParameter("transport") ?: error("Missing transport")
        val transport = ShareTransport.valueOf(transportParam)
        val key = uri.getQueryParameter("key")?.let { decode(it) } ?: error("Missing key")
        val driveFolderId = uri.getQueryParameter("driveFolderId")?.let { decode(it) }
        return ShareInvite(
            shareId = shareId,
            transport = transport,
            key = key,
            driveFolderId = driveFolderId,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun decode(value: String): String {
        return URLDecoder.decode(value, StandardCharsets.UTF_8.toString())
    }
}
