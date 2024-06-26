package ru.netology.nework.dao

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.netology.nework.db.AppDb

@InstallIn(SingletonComponent::class)
@Module
object DaoModule {
    @Provides
    fun providePostDao(db: AppDb): PostDao = db.postDao()

    @Provides
    fun providePostRemoteKeyDao(db: AppDb): PostRemoteKeyDao = db.postRemoteKeyDao()

    @Provides
    fun provideMyWallRemoteKeyDao(db: AppDb): WallRemoteKeyDao = db.wallRemoteKeyDao()

    @Provides
    fun provideEventDao(db: AppDb): EventDao = db.eventDao()

    @Provides
    fun provideEventRemoteKeyDao(db: AppDb): EventRemoteKeyDao = db.eventRemoteKeyDao()

    @Provides
    fun provideUserDao(db: AppDb): UserDao = db.userDao()

    @Provides
    fun provideJobDao(db: AppDb): JobDao = db.jobDao()
}