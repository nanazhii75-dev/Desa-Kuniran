package com.desa.kuniran.core.network

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

@JsonClass(generateAdapter = true)
data class RequestOtpRequest(
    val normalizedPhone: String // E.164 canonical (+62...)
)

@JsonClass(generateAdapter = true)
data class RequestOtpResponse(
    val success: Boolean,
    val message: String,
    val cooldownSeconds: Int,
    val expiresAt: Long
)

@JsonClass(generateAdapter = true)
data class VerifyOtpRequest(
    val normalizedPhone: String,
    val otpCode: String
)

@JsonClass(generateAdapter = true)
data class AuthTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val isNewUser: Boolean,
    val displayName: String? = null
)

@JsonClass(generateAdapter = true)
data class RefreshTokenRequest(
    val refreshToken: String
)

@JsonClass(generateAdapter = true)
data class RefreshTokenResponseDto(
    val accessToken: String,
    val refreshToken: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateGroupRequest(
    val name: String,
    val description: String,
    val groupType: String,
    val address: String,
    val joinApprovalRequired: Boolean
)

@JsonClass(generateAdapter = true)
data class AddMemberRequest(
    val normalizedPhone: String, // Server will compute HMAC and look up user safely
    val role: String
)

@JsonClass(generateAdapter = true)
data class CreateTransactionRequest(
    val type: String,
    val amountRupiah: Long,
    val categoryId: String,
    val categoryName: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class CreateComplaintRequest(
    val category: String,
    val description: String,
    val location: String
)

@JsonClass(generateAdapter = true)
data class CreateAnnouncementRequest(
    val title: String,
    val content: String,
    val isPinned: Boolean
)

@JsonClass(generateAdapter = true)
data class CreateActivityRequest(
    val title: String,
    val description: String,
    val dateText: String,
    val location: String
)

@JsonClass(generateAdapter = true)
data class RsvpActivityRequest(
    val status: String // GOING, MAYBE, NOT_GOING
)

interface DesaKuniranApiService {
    @Headers("${NetworkConstants.HEADER_NO_AUTH}: true")
    @POST("auth/request-otp")
    suspend fun requestOtp(@Body request: RequestOtpRequest): Response<RequestOtpResponse>

    @Headers("${NetworkConstants.HEADER_NO_AUTH}: true")
    @POST("auth/verify-otp")
    suspend fun verifyOtp(@Body request: VerifyOtpRequest): Response<AuthTokenResponse>

    @Headers("${NetworkConstants.HEADER_NO_AUTH}: true")
    @POST("auth/refresh-token")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<RefreshTokenResponseDto>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    @GET("groups")
    suspend fun getMyGroups(): Response<List<GroupNetworkDto>>

    @POST("groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): Response<GroupNetworkDto>

    @GET("groups/{groupId}")
    suspend fun getGroupDetail(@Path("groupId") groupId: String): Response<GroupNetworkDto>

    @GET("groups/{groupId}/members")
    suspend fun getGroupMembers(@Path("groupId") groupId: String): Response<List<GroupMemberNetworkDto>>

    @POST("groups/{groupId}/members")
    suspend fun addMember(
        @Path("groupId") groupId: String,
        @Body request: AddMemberRequest
    ): Response<GroupMemberNetworkDto>

    @DELETE("groups/{groupId}/members/{memberId}")
    suspend fun removeMember(
        @Path("groupId") groupId: String,
        @Path("memberId") memberId: String
    ): Response<Unit>

    @GET("groups/{groupId}/finance")
    suspend fun getFinanceSummary(@Path("groupId") groupId: String): Response<FinanceSummaryNetworkDto>

    @GET("groups/{groupId}/transactions")
    suspend fun getTransactions(@Path("groupId") groupId: String): Response<List<TransactionNetworkDto>>

    @POST("groups/{groupId}/transactions")
    suspend fun createTransaction(
        @Path("groupId") groupId: String,
        @Body request: CreateTransactionRequest
    ): Response<TransactionNetworkDto>

    @GET("groups/{groupId}/activities")
    suspend fun getActivities(@Path("groupId") groupId: String): Response<List<ActivityNetworkDto>>

    @POST("groups/{groupId}/activities")
    suspend fun createActivity(
        @Path("groupId") groupId: String,
        @Body request: CreateActivityRequest
    ): Response<ActivityNetworkDto>

    @POST("activities/{id}/participants")
    suspend fun rsvpActivity(
        @Path("id") activityId: String,
        @Body request: RsvpActivityRequest
    ): Response<Unit>

    @GET("groups/{groupId}/announcements")
    suspend fun getAnnouncements(@Path("groupId") groupId: String): Response<List<AnnouncementNetworkDto>>

    @POST("groups/{groupId}/announcements")
    suspend fun createAnnouncement(
        @Path("groupId") groupId: String,
        @Body request: CreateAnnouncementRequest
    ): Response<AnnouncementNetworkDto>

    @GET("groups/{groupId}/complaints")
    suspend fun getComplaints(@Path("groupId") groupId: String): Response<List<ComplaintNetworkDto>>

    @POST("groups/{groupId}/complaints")
    suspend fun createComplaint(
        @Path("groupId") groupId: String,
        @Body request: CreateComplaintRequest
    ): Response<ComplaintNetworkDto>

    @GET("notifications")
    suspend fun getNotifications(): Response<List<NotificationNetworkDto>>

    @POST("notifications/{id}/mark-read")
    suspend fun markNotificationRead(@Path("id") id: String): Response<Unit>

    @POST("notifications/{id}/mark-informed-manually")
    suspend fun markInformedManually(@Path("id") id: String): Response<Unit>
}

@JsonClass(generateAdapter = true)
data class GroupNetworkDto(
    val id: String,
    val name: String,
    val description: String,
    val avatarUrl: String?,
    val groupType: String,
    val address: String,
    val createdBy: String,
    val status: String,
    val memberCount: Int,
    val activityCount: Int,
    val announcementCount: Int,
    val joinApprovalRequired: Boolean,
    val groupCode: String,
    val createdAt: Long,
    val updatedAt: Long
)

@JsonClass(generateAdapter = true)
data class GroupMemberNetworkDto(
    val id: String,
    val groupId: String,
    val userId: String,
    val displayName: String,
    val maskedPhone: String,
    val role: String,
    val status: String,
    val joinedAt: Long,
    val invitedBy: String?,
    val needsManualNotice: Boolean
)

@JsonClass(generateAdapter = true)
data class FinanceSummaryNetworkDto(
    val balanceRupiah: Long,
    val incomeRupiah: Long,
    val expenseRupiah: Long,
    val transactionCount: Int
)

@JsonClass(generateAdapter = true)
data class TransactionNetworkDto(
    val id: String,
    val groupId: String,
    val accountId: String,
    val type: String,
    val amountRupiah: Long,
    val categoryId: String,
    val categoryName: String,
    val description: String,
    val createdBy: String,
    val createdByName: String,
    val approvedBy: String?,
    val status: String,
    val occurredAt: Long,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class ActivityNetworkDto(
    val id: String,
    val groupId: String,
    val title: String,
    val description: String,
    val dateText: String,
    val location: String,
    val participantsCount: Int,
    val committeeCount: Int,
    val checklist: List<String>,
    val userStatus: String,
    val createdAt: Long
)

@JsonClass(generateAdapter = true)
data class AnnouncementNetworkDto(
    val id: String,
    val groupId: String,
    val title: String,
    val content: String,
    val authorName: String,
    val isPinned: Boolean,
    val publishedAt: Long,
    val readCount: Int,
    val totalRecipients: Int
)

@JsonClass(generateAdapter = true)
data class ComplaintNetworkDto(
    val id: String,
    val groupId: String,
    val authorId: String,
    val authorName: String,
    val category: String,
    val description: String,
    val location: String,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val officerNotes: String?
)

@JsonClass(generateAdapter = true)
data class NotificationNetworkDto(
    val id: String,
    val type: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean,
    val channelUsed: String
)
