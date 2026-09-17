package com.desa.kuniran.core.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Event status sesi autentikasi warga desa.
 */
sealed interface SessionEvent {
    data object SessionExpired : SessionEvent
    data class UserLoggedIn(val userId: String) : SessionEvent
    data object UserLoggedOut : SessionEvent
}

/**
 * Pengelola status sesi dan notifikasi kadaluarsa sesi.
 * Menjamin UI dapat merespon saat token refresh ditolak tanpa exception unhandled.
 */
interface SessionManager {
    val sessionEvents: SharedFlow<SessionEvent>
    fun notifySessionExpired()
    fun notifyUserLoggedIn(userId: String)
    fun notifyUserLoggedOut()
}

class SessionManagerImpl : SessionManager {
    private val _sessionEvents = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 8)
    override val sessionEvents: SharedFlow<SessionEvent> = _sessionEvents.asSharedFlow()

    override fun notifySessionExpired() {
        _sessionEvents.tryEmit(SessionEvent.SessionExpired)
    }

    override fun notifyUserLoggedIn(userId: String) {
        _sessionEvents.tryEmit(SessionEvent.UserLoggedIn(userId))
    }

    override fun notifyUserLoggedOut() {
        _sessionEvents.tryEmit(SessionEvent.UserLoggedOut)
    }
}
