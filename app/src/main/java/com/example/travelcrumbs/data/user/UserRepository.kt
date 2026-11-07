package com.example.travelcrumbs.data.user

import com.example.travelcrumbs.data.remote.AppApi
import com.example.travelcrumbs.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

/** 로그인된 사용자 정보의 창구 — 화면 · ViewModel 은 AppApi 를 직접 부르지 않고 여기만 부름. */
interface UserRepository {
    /** 서버가 꺼져 있거나 실패하면 Result.failure. */
    suspend fun myProfile(): Result<UserProfile>
}

@Singleton
class DefaultUserRepository @Inject constructor(
    private val api: AppApi,
) : UserRepository {

    override suspend fun myProfile(): Result<UserProfile> = runCatching {
        val me = api.getMe()
        UserProfile(userNo = me.userNo, nickname = me.nicknameNm)
    }
}
