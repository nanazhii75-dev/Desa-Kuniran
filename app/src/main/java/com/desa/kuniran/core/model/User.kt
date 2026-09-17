package com.desa.kuniran.core.model

data class User(
    val id: String,
    val displayName: String,
    val maskedPhone: String,
    val avatarUrl: String? = null,
    val accountStatus: AccountStatus = AccountStatus.ACTIVE,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = System.currentTimeMillis()
)

enum class AccountStatus {
    ACTIVE,
    PENDING_VERIFICATION,
    SUSPENDED
}
