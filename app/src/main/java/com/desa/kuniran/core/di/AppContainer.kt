package com.desa.kuniran.core.di

import android.content.Context
import com.desa.kuniran.core.database.DesaKuniranDatabase
import com.desa.kuniran.core.network.DesaKuniranApiService
import com.desa.kuniran.core.network.NetworkClientFactory
import com.desa.kuniran.core.network.SessionManager
import com.desa.kuniran.core.network.SessionManagerImpl
import com.desa.kuniran.core.security.EncryptedTokenStorage
import com.desa.kuniran.core.security.PhoneProtector
import com.desa.kuniran.core.security.PhoneProtectorImpl
import com.desa.kuniran.core.security.TokenStorage
import com.desa.kuniran.data.repository.ActivityRepository
import com.desa.kuniran.data.repository.ActivityRepositoryImpl
import com.desa.kuniran.data.repository.AnnouncementRepository
import com.desa.kuniran.data.repository.AnnouncementRepositoryImpl
import com.desa.kuniran.data.repository.AuthRepository
import com.desa.kuniran.data.repository.AuthRepositoryImpl
import com.desa.kuniran.data.repository.ComplaintRepository
import com.desa.kuniran.data.repository.ComplaintRepositoryImpl
import com.desa.kuniran.data.repository.FinanceRepository
import com.desa.kuniran.data.repository.FinanceRepositoryImpl
import com.desa.kuniran.data.repository.GroupRepository
import com.desa.kuniran.data.repository.GroupRepositoryImpl
import com.desa.kuniran.data.repository.MemberRepository
import com.desa.kuniran.data.repository.MemberRepositoryImpl
import com.desa.kuniran.data.repository.NotificationRepository
import com.desa.kuniran.data.repository.NotificationRepositoryImpl
import com.desa.kuniran.data.repository.VillageFirestoreRepository
import com.desa.kuniran.data.repository.VillageFirestoreRepositoryImpl
import com.squareup.moshi.Moshi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient

interface AppContainer {
    val phoneProtector: PhoneProtector
    val tokenStorage: TokenStorage
    val sessionManager: SessionManager
    val authRepository: AuthRepository
    val groupRepository: GroupRepository
    val memberRepository: MemberRepository
    val financeRepository: FinanceRepository
    val activityRepository: ActivityRepository
    val announcementRepository: AnnouncementRepository
    val complaintRepository: ComplaintRepository
    val notificationRepository: NotificationRepository
    val villageFirestoreRepository: VillageFirestoreRepository
    val themeManager: com.desa.kuniran.core.designsystem.ThemeManager
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    private val database: DesaKuniranDatabase by lazy {
        DesaKuniranDatabase.getInstance(context)
    }

    override val phoneProtector: PhoneProtector by lazy {
        PhoneProtectorImpl()
    }

    override val tokenStorage: TokenStorage by lazy {
        EncryptedTokenStorage(context)
    }

    override val sessionManager: SessionManager by lazy {
        SessionManagerImpl()
    }

    private val moshi: Moshi by lazy {
        NetworkClientFactory.createMoshi()
    }

    private val okHttpClient: OkHttpClient by lazy {
        NetworkClientFactory.createOkHttpClient(
            tokenStorage = tokenStorage,
            sessionManager = sessionManager,
            moshi = moshi
        )
    }

    private val apiService: DesaKuniranApiService by lazy {
        NetworkClientFactory.createRetrofit(
            okHttpClient = okHttpClient,
            moshi = moshi
        ).create(DesaKuniranApiService::class.java)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(apiService, database.userDao(), phoneProtector, tokenStorage)
    }

    override val groupRepository: GroupRepository by lazy {
        GroupRepositoryImpl(database.groupDao())
    }

    override val memberRepository: MemberRepository by lazy {
        MemberRepositoryImpl(database.memberDao(), phoneProtector, villageFirestoreRepository)
    }

    override val financeRepository: FinanceRepository by lazy {
        FinanceRepositoryImpl(database.transactionDao(), database.monthlyBalanceDao())
    }

    override val activityRepository: ActivityRepository by lazy {
        ActivityRepositoryImpl(database.activityDao())
    }

    override val announcementRepository: AnnouncementRepository by lazy {
        AnnouncementRepositoryImpl(
            announcementDao = database.announcementDao(),
            firestoreRepo = villageFirestoreRepository,
            notificationRepository = notificationRepository,
            context = context
        )
    }

    override val complaintRepository: ComplaintRepository by lazy {
        ComplaintRepositoryImpl(database.complaintDao(), villageFirestoreRepository)
    }

    override val notificationRepository: NotificationRepository by lazy {
        NotificationRepositoryImpl(database.notificationDao())
    }

    override val villageFirestoreRepository: VillageFirestoreRepository by lazy {
        VillageFirestoreRepositoryImpl()
    }

    override val themeManager: com.desa.kuniran.core.designsystem.ThemeManager by lazy {
        com.desa.kuniran.core.designsystem.ThemeManager(context)
    }

    init {
        // Inisialisasi data awal lokal di database Room saat pertama kali buka
        CoroutineScope(Dispatchers.IO).launch {
            try {
                groupRepository.initializeDefaultGroupsIfEmpty()
                memberRepository.initializeDefaultMembersIfEmpty("group_rt02")
                financeRepository.initializeDefaultFinanceIfEmpty("group_rt02")
                activityRepository.initializeDefaultActivitiesIfEmpty("group_rt02")
                announcementRepository.initializeDefaultAnnouncementsIfEmpty("group_rt02")
                complaintRepository.initializeDefaultComplaintsIfEmpty("group_rt02")
                notificationRepository.initializeDefaultNotificationsIfEmpty()
            } catch (e: Throwable) {
                android.util.Log.e("DesaKuniran", "Initialization error: ${e.message}", e)
            }
        }
    }
}
