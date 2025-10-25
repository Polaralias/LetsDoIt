package com.letsdoit.app.share

import javax.inject.Inject

interface DriveAccountProvider {
    suspend fun getAccounts(): List<DriveAccount>
}

class DefaultDriveAccountProvider @Inject constructor() : DriveAccountProvider {
    override suspend fun getAccounts(): List<DriveAccount> {
        return listOf(
            DriveAccount(email = "planner@letsdoit.test", displayName = "Planner"),
            DriveAccount(email = "tasks@letsdoit.test", displayName = "Tasks")
        )
    }
}
