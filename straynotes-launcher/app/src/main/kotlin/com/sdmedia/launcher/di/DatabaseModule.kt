package com.sdmedia.launcher.di

import android.content.Context
import androidx.room.Room
import com.sdmedia.launcher.data.db.AppDao
import com.sdmedia.launcher.data.db.LauncherDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLauncherDatabase(@ApplicationContext context: Context): LauncherDatabase =
        Room.databaseBuilder(
            context,
            LauncherDatabase::class.java,
            "launcher_db"
        ).fallbackToDestructiveMigration().build()

    @Provides
    fun provideAppDao(db: LauncherDatabase): AppDao = db.appDao()
}
