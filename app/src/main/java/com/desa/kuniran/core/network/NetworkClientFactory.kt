package com.desa.kuniran.core.network

import com.desa.kuniran.BuildConfig
import com.desa.kuniran.core.security.TokenStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory penyedia komponen jaringan HTTP Retrofit & OkHttp untuk Desa Kuniran.
 * Menjamin konfigurasi terpusat, konsisten, dan memenuhi standar keamanan Aturan Mutlak #7.
 */
object NetworkClientFactory {

    fun createMoshi(): Moshi {
        return Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    fun createOkHttpClient(
        tokenStorage: TokenStorage,
        sessionManager: SessionManager,
        moshi: Moshi,
        baseUrl: String = NetworkConstants.DEFAULT_BASE_URL,
        isDebug: Boolean = BuildConfig.DEBUG
    ): OkHttpClient {
        val authInterceptor = AuthInterceptor(tokenStorage = tokenStorage)
        val tokenAuthenticator = TokenAuthenticator(
            tokenStorage = tokenStorage,
            sessionManager = sessionManager,
            moshi = moshi,
            baseUrl = baseUrl
        )
        val loggingInterceptor = SafeLoggingInterceptor.create(isDebug = isDebug)

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(NetworkConstants.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(NetworkConstants.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(NetworkConstants.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun createRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi,
        baseUrl: String = NetworkConstants.DEFAULT_BASE_URL
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    fun createApiService(
        tokenStorage: TokenStorage,
        sessionManager: SessionManager,
        baseUrl: String = NetworkConstants.DEFAULT_BASE_URL,
        isDebug: Boolean = BuildConfig.DEBUG
    ): DesaKuniranApiService {
        val moshi = createMoshi()
        val okHttpClient = createOkHttpClient(
            tokenStorage = tokenStorage,
            sessionManager = sessionManager,
            moshi = moshi,
            baseUrl = baseUrl,
            isDebug = isDebug
        )
        val retrofit = createRetrofit(
            okHttpClient = okHttpClient,
            moshi = moshi,
            baseUrl = baseUrl
        )
        return retrofit.create(DesaKuniranApiService::class.java)
    }
}
