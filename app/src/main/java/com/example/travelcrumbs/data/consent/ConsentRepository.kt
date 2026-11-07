package com.example.travelcrumbs.data.consent

import com.example.travelcrumbs.data.remote.AppApi
import com.example.travelcrumbs.data.remote.ConsentCreateRequest
import com.example.travelcrumbs.domain.model.ConsentType
import javax.inject.Inject
import javax.inject.Singleton

/** 동의 상태의 창구(user_consent_hist) — 최신 이력 1건이 지금 상태. */
interface ConsentRepository {
    /** 지금 동의한 상태인지 — 이력이 없거나 마지막이 철회(N)면 false. */
    suspend fun isAgreed(type: ConsentType): Result<Boolean>

    /** 동의를 기록(이력 1건 추가). */
    suspend fun agree(type: ConsentType): Result<Unit>
}

@Singleton
class DefaultConsentRepository @Inject constructor(
    private val api: AppApi,
) : ConsentRepository {

    override suspend fun isAgreed(type: ConsentType): Result<Boolean> = runCatching {
        api.getLatestConsent(type.code).consentYn == YES
    }

    override suspend fun agree(type: ConsentType): Result<Unit> = runCatching {
        api.createConsent(ConsentCreateRequest(consentTypeCd = type.code, consentYn = YES))
        Unit
    }

    private companion object {
        const val YES = "Y"
    }
}
