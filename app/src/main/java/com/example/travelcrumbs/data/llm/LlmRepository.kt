package com.example.travelcrumbs.data.llm

import javax.inject.Inject
import javax.inject.Singleton

/** 화면 · ViewModel 이 LLM 에 닿는 유일한 길. */
interface LlmRepository {
    suspend fun ask(prompt: String): String
}

@Singleton
class DefaultLlmRepository @Inject constructor(
    private val dataSource: LlmDataSource,
) : LlmRepository {

    override suspend fun ask(prompt: String): String = dataSource.generate(prompt)
}
