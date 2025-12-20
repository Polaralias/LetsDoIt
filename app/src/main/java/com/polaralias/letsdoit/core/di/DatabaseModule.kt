package com.polaralias.letsdoit.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.polaralias.letsdoit.data.local.AppDatabase
import com.polaralias.letsdoit.data.local.dao.FolderDao
import com.polaralias.letsdoit.data.local.dao.ListDao
import com.polaralias.letsdoit.data.local.dao.SpaceDao
import com.polaralias.letsdoit.data.local.dao.TaskDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN calendarEventId INTEGER")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN recurrenceRule TEXT")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "lets_do_it_db"
        )
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
    }

    @Provides
    fun provideSpaceDao(database: AppDatabase): SpaceDao = database.spaceDao()

    @Provides
    fun provideFolderDao(database: AppDatabase): FolderDao = database.folderDao()

    @Provides
    fun provideListDao(database: AppDatabase): ListDao = database.listDao()

    @Provides
    fun provideTaskDao(database: AppDatabase): TaskDao = database.taskDao()
}
