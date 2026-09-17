package com.desa.kuniran.core.network

import com.desa.kuniran.core.security.TokenStorage
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AuthInterceptorTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var fakeTokenStorage: FakeTokenStorage

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        fakeTokenStorage = FakeTokenStorage()
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `when token exists, attaches Bearer authorization header`() {
        fakeTokenStorage.saveTokens(
            accessToken = "secure_access_token_123",
            refreshToken = "refresh_token_456",
            userId = "user_jogorejo_01"
        )

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(fakeTokenStorage, appVersionName = "1.0"))
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/groups"))
            .build()

        val response = client.newCall(request).execute()
        assertEquals(200, response.code)

        val recordedRequest = mockWebServer.takeRequest()
        assertEquals("Bearer secure_access_token_123", recordedRequest.getHeader("Authorization"))
        assertEquals("Android", recordedRequest.getHeader("X-Platform"))
        assertEquals("1.0", recordedRequest.getHeader("X-App-Version"))
        assertEquals("application/json", recordedRequest.getHeader("Accept"))
    }

    @Test
    fun `when endpoint is no-auth, skips authorization header and strips internal header`() {
        fakeTokenStorage.saveTokens(
            accessToken = "secure_access_token_123",
            refreshToken = "refresh_token_456",
            userId = "user_jogorejo_01"
        )

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(fakeTokenStorage, appVersionName = "1.0"))
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/auth/request-otp"))
            .header(NetworkConstants.HEADER_NO_AUTH, "true")
            .build()

        val response = client.newCall(request).execute()
        assertEquals(200, response.code)

        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
        assertNull(recordedRequest.getHeader(NetworkConstants.HEADER_NO_AUTH))
    }

    @Test
    fun `when token is empty, proceeds without authorization header`() {
        fakeTokenStorage.clearTokens()

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(fakeTokenStorage, appVersionName = "1.0"))
            .build()

        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("{}"))

        val request = Request.Builder()
            .url(mockWebServer.url("/public-info"))
            .build()

        val response = client.newCall(request).execute()
        assertEquals(200, response.code)

        val recordedRequest = mockWebServer.takeRequest()
        assertNull(recordedRequest.getHeader("Authorization"))
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
