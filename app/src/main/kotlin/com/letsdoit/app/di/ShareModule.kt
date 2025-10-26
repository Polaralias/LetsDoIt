package com.letsdoit.app.di

import com.letsdoit.app.share.DefaultDriveAccountProvider
import com.letsdoit.app.share.DefaultDriveService
import com.letsdoit.app.share.DriveAccountProvider
import com.letsdoit.app.share.DriveService
import com.letsdoit.app.share.transport.DriveAppDataTransport
import com.letsdoit.app.share.transport.DriveTransport
import com.letsdoit.app.share.transport.NearbyTransport
import com.letsdoit.app.share.transport.StubNearbyTransport
import com.letsdoit.app.share.sync.DeviceIdProvider
import com.letsdoit.app.share.sync.DeviceIdSource
import com.letsdoit.app.share.sync.LamportClock
import com.letsdoit.app.share.sync.LamportCounter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ShareModule {
    @Binds
    abstract fun bindDriveService(service: DefaultDriveService): DriveService

    @Binds
    abstract fun bindDriveAccountProvider(provider: DefaultDriveAccountProvider): DriveAccountProvider

    @Binds
    abstract fun bindDriveTransport(transport: DriveAppDataTransport): DriveTransport

    @Binds
    abstract fun bindNearbyTransport(transport: StubNearbyTransport): NearbyTransport

    @Binds
    abstract fun bindDeviceIdSource(provider: DeviceIdProvider): DeviceIdSource

    @Binds
    abstract fun bindLamportCounter(counter: LamportClock): LamportCounter
}
