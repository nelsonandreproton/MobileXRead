package com.mobilexread.di

import android.content.Context
import androidx.room.Room
import com.mobilexread.data.local.AppDatabase
import com.mobilexread.data.local.ArticleDao
import com.mobilexread.data.local.ProcessingLogDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "mobilexread.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

    @Provides
    fun provideArticleDao(db: AppDatabase): ArticleDao = db.articleDao()

    @Provides
    fun provideProcessingLogDao(db: AppDatabase): ProcessingLogDao = db.processingLogDao()
}
