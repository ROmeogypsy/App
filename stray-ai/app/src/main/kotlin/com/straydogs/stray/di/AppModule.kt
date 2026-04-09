package com.straydogs.stray.di

import android.content.Context
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.straydogs.stray.data.db.StrayDatabase
import com.straydogs.stray.data.remote.HuggingFaceApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                }
            )
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api-inference.huggingface.co/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideHuggingFaceApi(retrofit: Retrofit): HuggingFaceApi =
        retrofit.create(HuggingFaceApi::class.java)

    @Provides
    @Singleton
    fun provideStrayDatabase(@ApplicationContext context: Context): StrayDatabase =
        Room.databaseBuilder(
            context,
            StrayDatabase::class.java,
            "stray_db"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideConversationDao(db: StrayDatabase) = db.conversationDao()

    @Provides
    fun provideMessageDao(db: StrayDatabase) = db.messageDao()
}
