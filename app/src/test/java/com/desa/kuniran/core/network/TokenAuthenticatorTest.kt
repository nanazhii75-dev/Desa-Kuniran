package com.desa.kuniran.core.network

import com.desa.kuniran.core.security.TokenStorage
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TokenAuthenticatorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var fakeTokenStorage: FakeTokenStorage
    private lateinit var sessionManager: SessionManager
    private lateinit var moshi: Moshi

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        fakeTokenStorage = FakeTokenStorage()
        sessionManager = SessionManagerImpl()
        moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `when 401 received, refreshes token and retries request with new token`() {
        fakeTokenStorage.saveTokens(
            accessToken = "expired_token_111",
            refreshToken = "valid_refresh_token_222",
            userId = "user_rt02"
        )

        val baseUrl = mockWebServer.url("/").toString()
        val authenticator = TokenAuthenticator(
            tokenStorage = fakeTokenStorage,
            sessionManager = sessionManager,
            moshi = moshi,
            baseUrl = baseUrl
        )
        val interceptor = AuthInterceptor(fakeTokenStorage)

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .authenticator(authenticator)
            .build()

        // 1. Initial request gets 401
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("{\"message\":\"Unauthorized\"}"))
        // 2. Token refresh request gets 200 with new tokens
        mockWebServer.enqueue(
            MockResponse().setResponseCode(200).setBody(
                "{\"accessToken\":\"brand_new_token_333\",\"refreshToken\":\"new_refresh_token_444\"}"
            )
        )
        // 3. Retried original request gets 200
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{\"success\":true}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/groups/group_rt02/finance"))
            .build()

        val response = client.newCall(request).execute()
        assertEquals(200, response.code)

        // Verify token in storage was updated
        assertEquals("brand_new_token_333", fakeTokenStorage.getAccessToken())
        assertEquals("new_refresh_token_444", fakeTokenStorage.getRefreshToken())

        // Verify request flow in MockWebServer
        val firstRequest = mockWebServer.takeRequest()
        assertEquals("Bearer expired_token_111", firstRequest.getHeader("Authorization"))

        val refreshRequest = mockWebServer.takeRequest()
        assertEquals("/auth/refresh-token", refreshRequest.path)
        assertTrue(refreshRequest.body.readUtf8().contains("valid_refresh_token_222"))

        val retriedRequest = mockWebServer.takeRequest()
        assertEquals("Bearer brand_new_token_333", retriedRequest.getHeader("Authorization"))
    }

    @Test
    fun `when refresh token also fails, clears session and triggers session expired`() {
        fakeTokenStorage.saveTokens(
            accessToken = "expired_token_111",
            refreshToken = "invalid_refresh_token",
            userId = "user_rt02"
        )

        val baseUrl = mockWebServer.url("/").toString()
        val authenticator = TokenAuthenticator(
            tokenStorage = fakeTokenStorage,
            sessionManager = sessionManager,
            moshi = moshi,
            baseUrl = baseUrl
        )
        val interceptor = AuthInterceptor(fakeTokenStorage)

        val client = OkHttpClient.Builder()
            .addInterceptor(interceptor)
            .authenticator(authenticator)
            .build()

        // 1. Initial request gets 401
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("{\"message\":\"Unauthorized\"}"))
        // 2. Token refresh request gets 401
        mockWebServer.enqueue(MockResponse().setResponseCode(401).setBody("{\"message\":\"Refresh token expired\"}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/groups"))
            .build()

        val response = client.newCall(request).execute()
        assertEquals(401, response.code)

        // Verify storage was cleared
        assertTrue(!fakeTokenStorage.hasSession())
    }

    private class FakeTokenStorage : TokenStorage {
        private var access: String? = null
        private var refresh: String? = null
        private var user: String? = null

        override fun saveTokens(accessToken: String, refreshToken: String, userId: String) {
            access = accessToken
            refresh = refreshToken
            user = userId
        }

        override fun updateAccessToken(accessToken: String) {
            access = accessToken
        }

        override fun getAccessToken(): String? = access

        override fun getRefreshToken(): String? = refresh

        override fun getUserId(): String? = user

        override fun clearTokens() {
            access = null
            refresh = null
            user = null
        }

        override fun hasSession(): Boolean = !access.isNullOrBlank()
    }
}
