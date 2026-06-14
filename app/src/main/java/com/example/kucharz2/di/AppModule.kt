package com.example.kucharz2.di

import android.content.Context
import androidx.room.Room
import com.example.kucharz2.BuildConfig
import com.example.kucharz2.data.KucharzDao
import com.example.kucharz2.data.KucharzDatabase
import com.example.kucharz2.data.RecipeApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, gson: Gson): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.RECIPE_API_BASE_URL.ensureTrailingSlash())
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    @Provides
    @Singleton
    fun provideRecipeApi(retrofit: Retrofit): RecipeApi = retrofit.create(RecipeApi::class.java)

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KucharzDatabase = Room.databaseBuilder(
        context,
        KucharzDatabase::class.java,
        "kucharz.db"
    ).build()

    @Provides
    fun provideDao(database: KucharzDatabase): KucharzDao = database.dao()
}

private fun String.ensureTrailingSlash(): String = if (endsWith('/')) this else "$this/"
