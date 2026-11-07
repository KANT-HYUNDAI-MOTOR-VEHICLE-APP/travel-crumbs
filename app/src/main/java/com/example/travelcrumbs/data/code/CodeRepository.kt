package com.example.travelcrumbs.data.code

import com.example.travelcrumbs.data.remote.AppApi
import com.example.travelcrumbs.domain.model.CodeItem
import javax.inject.Inject
import javax.inject.Singleton

/** 공통 코드(cd_grp_mst · cd_dtl)의 창구 — 버튼 글자를 앱에 박지 않고 서버 코드명을 씀. */
interface CodeRepository {
    suspend fun codes(groupCode: String): Result<List<CodeItem>>
}

@Singleton
class DefaultCodeRepository @Inject constructor(
    private val api: AppApi,
) : CodeRepository {

    override suspend fun codes(groupCode: String): Result<List<CodeItem>> = runCatching {
        api.getCodes(groupCode).map { CodeItem(code = it.cd, label = it.cdNm) }
    }
}
