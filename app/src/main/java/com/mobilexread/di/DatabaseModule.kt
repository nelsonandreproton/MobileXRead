package com.mobilexread.di

import android.content.Context
import androidx.room.Room
import com.mobilexread.data.local.AppDatabase
import com.mobilexread.data.local.ArticleDao
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
        Room.databaseBuilder(context, AppDatabase::class.java, "mobilexread.db").build()

    @Provides
    fun provideArticleDao(db: AppDatabase): ArticleDao = db.articleDao()
}
