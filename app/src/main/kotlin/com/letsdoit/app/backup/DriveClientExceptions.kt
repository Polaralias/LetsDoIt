package com.letsdoit.app.backup

open class DriveClientException(val code: Int, message: String?) : Exception(message)

class DriveAuthException(code: Int, message: String?) : DriveClientException(code, message)
