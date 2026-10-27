package dev.dvach.client.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.dvach.client.data.ChanRepository
import dev.dvach.client.data.NetworkChanRepository
import dev.dvach.client.data.local.ChanDao
import dev.dvach.client.data.local.ChanDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): ChanDatabase =
        Room.databaseBuilder(context, ChanDatabase::class.java, "dvach.db").build()

    @Provides
    fun provideDao(db: ChanDatabase): ChanDao = db.chanDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingsModule {

    @Binds
    @Singleton
    abstract fun bindChanRepository(impl: NetworkChanRepository): ChanRepository
}
