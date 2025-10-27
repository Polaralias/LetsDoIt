package com.polaralias.letsdoit.di

import com.polaralias.letsdoit.share.DefaultDriveAccountProvider
import com.polaralias.letsdoit.share.DefaultDriveService
import com.polaralias.letsdoit.share.DriveAccountProvider
import com.polaralias.letsdoit.share.DriveService
import com.polaralias.letsdoit.share.transport.DriveAppDataTransport
import com.polaralias.letsdoit.share.transport.DriveTransport
import com.polaralias.letsdoit.share.transport.NearbyTransport
import com.polaralias.letsdoit.share.transport.StubNearbyTransport
import com.polaralias.letsdoit.share.sync.DeviceIdProvider
import com.polaralias.letsdoit.share.sync.DeviceIdSource
import com.polaralias.letsdoit.share.sync.LamportClock
import com.polaralias.letsdoit.share.sync.LamportCounter
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
