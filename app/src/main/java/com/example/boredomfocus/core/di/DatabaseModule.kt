package com.example.boredomfocus.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.boredomfocus.data.local.database.BoredomDatabase
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
    fun provideBoredomDatabase(
        @ApplicationContext context: Context
    ) : RoomDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            BoredomDatabase::class.java,
            "boredom_database"
        ).build()
    }

}