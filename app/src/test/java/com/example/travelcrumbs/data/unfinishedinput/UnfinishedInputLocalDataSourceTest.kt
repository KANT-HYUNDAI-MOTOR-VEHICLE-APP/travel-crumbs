package com.example.travelcrumbs.data.unfinishedinput

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.travelcrumbs.data.local.AppPreferencesKeys
import com.example.travelcrumbs.domain.model.UnfinishedInput
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** 실제 DataStore(임시 파일)로 작성 중 내용의 새 키 · #6 판 옛 키 버림(D27)을 확인. */
class UnfinishedInputLocalDataSourceTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = scope,
        produceFile = { File(folder.root, "${AppPreferencesKeys.FILE_NAME}.preferences_pb") },
    )
    private val local = DataStoreUnfinishedInputLocalDataSource(dataStore, Json)

    @After
    fun tearDown() {
        scope.cancel()
    }

    @Test
    fun `V38 옛 키에 #6 판 작성 중 내용 — 읽지 않음 · 띠 없음 · 오류 없음 · 옛 키 지워짐`() = runBlocking {
        val legacyJson = """{"destinationId":"trips","fields":{"memo":"바다"},"savedAtMillis":1}"""
        dataStore.edit { it[AppPreferencesKeys.Legacy.SHELL_INPUT_JSON] = legacyJson }

        assertNull(local.unfinishedInput.first())
        assertNull(dataStore.data.first()[AppPreferencesKeys.Legacy.SHELL_INPUT_JSON])
    }

    @Test
    fun `V27 새 키에 남기고 다시 읽음 · 비우면 없음`() = runBlocking {
        val input = UnfinishedInput("trips", mapOf("memo" to "바다"), savedAtMillis = 42L)

        local.save(input)
        assertEquals(input, local.unfinishedInput.first())

        local.clear()
        assertNull(local.unfinishedInput.first())
    }
}
