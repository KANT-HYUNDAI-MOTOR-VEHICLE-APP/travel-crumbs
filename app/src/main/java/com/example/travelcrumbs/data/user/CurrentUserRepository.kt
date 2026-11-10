package com.example.travelcrumbs.data.user

import com.example.travelcrumbs.data.local.TravelDao
import com.example.travelcrumbs.data.local.UserConsentHistEntity
import com.example.travelcrumbs.data.local.UserMstEntity
import com.example.travelcrumbs.data.local.YesNo
import com.example.travelcrumbs.domain.AppConfig
import com.example.travelcrumbs.domain.TimeSource
import com.example.travelcrumbs.domain.model.ConsentState
import com.example.travelcrumbs.domain.model.ConsentType
import com.example.travelcrumbs.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 지금 사용자 · 닉네임 · 동의의 유일한 출처(#51 U1 · C-02 인사 · C-03 설정 · A-01 위치 동의 · A-06 추천 공개 동의).
 *
 * 로그인 화면(C-01)은 다른 사람이 만듦 — 그 전까지 이 구현은 기기 안 기본 사용자 1명([AppConfig.LOCAL_USER_NO] ·
 * 닉네임 [AppConfig.DEFAULT_NICKNAME_NM])을 처음 읽을 때 만들어 씀. **로그인 담당은 이 인터페이스의 구현만 바꿈**
 * (로그인한 사용자 번호 · 서버 user_mst · user_consent_hist — di/UserModule 의 바인딩 한 줄). 데이터 원본은 서버(ADR 0015).
 */
interface CurrentUserRepository {
    /** 지금 사용자 — 닉네임이 바뀌면 새 값. */
    val user: Flow<UserProfile>

    /** 동의 유형별 최신 상태 — 한 번도 묻지 않은 유형은 없음. */
    val consents: Flow<Map<ConsentType, ConsentState>>

    /** 지금 사용자 번호(여행 · 동의 행에 붙음). */
    suspend fun currentUserNo(): Long

    /** 닉네임 바꾸기(C-03) — 앞뒤 공백을 지운 값 · 비었으면 바꾸지 않고 false. */
    suspend fun updateNickname(nicknameNm: String): Boolean

    /** 동의 · 철회 한 건을 이력에 남김(R12 · R14). */
    suspend fun recordConsent(type: ConsentType, agreed: Boolean)
}

@Singleton
class LocalCurrentUserRepository @Inject constructor(
    private val dao: TravelDao,
    private val timeSource: TimeSource,
) : CurrentUserRepository {

    private val ensureLock = Mutex()

    @Volatile
    private var ensured = false

    override val user: Flow<UserProfile> = flow {
        val userNo = currentUserNo()
        emitAll(dao.user(userNo).filterNotNull().map { UserProfile(it.userNo, it.nicknameNm) })
    }

    override val consents: Flow<Map<ConsentType, ConsentState>> = flow {
        val userNo = currentUserNo()
        emitAll(dao.consents(userNo).map(::latestByType))
    }

    override suspend fun currentUserNo(): Long {
        if (!ensured) {
            ensureLock.withLock {
                if (!ensured) {
                    if (dao.userOnce(AppConfig.LOCAL_USER_NO) == null) {
                        val now = timeSource.nowMillis()
                        dao.insertUser(
                            UserMstEntity(
                                userNo = AppConfig.LOCAL_USER_NO,
                                nicknameNm = AppConfig.DEFAULT_NICKNAME_NM,
                                createdAt = now,
                                createdUserNo = AppConfig.LOCAL_USER_NO,
                                updatedAt = now,
                                updatedUserNo = AppConfig.LOCAL_USER_NO,
                            ),
                        )
                    }
                    ensured = true
                }
            }
        }
        return AppConfig.LOCAL_USER_NO
    }

    override suspend fun updateNickname(nicknameNm: String): Boolean {
        val trimmed = nicknameNm.trim().take(AppConfig.NICKNAME_MAX_LENGTH)
        if (trimmed.isEmpty()) return false
        dao.updateNickname(currentUserNo(), trimmed, timeSource.nowMillis())
        return true
    }

    override suspend fun recordConsent(type: ConsentType, agreed: Boolean) {
        val userNo = currentUserNo()
        val now = timeSource.nowMillis()
        dao.insertConsent(
            UserConsentHistEntity(
                userNo = userNo,
                consentTypeCd = type.name,
                consentYn = YesNo.of(agreed),
                createdAt = now,
                createdUserNo = userNo,
                updatedAt = now,
                updatedUserNo = userNo,
            ),
        )
    }
}

/** 동의 이력(오래된 순) → 유형별 마지막 행. 모르는 코드는 버림. */
internal fun latestByType(rows: List<UserConsentHistEntity>): Map<ConsentType, ConsentState> =
    rows.mapNotNull { row ->
        ConsentType.entries.firstOrNull { it.name == row.consentTypeCd }
            ?.let { ConsentState(it, row.consentYn == YesNo.YES, row.createdAt) }
    }.associateBy { it.type }
