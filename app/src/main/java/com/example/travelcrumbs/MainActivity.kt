package com.example.travelcrumbs

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.travelcrumbs.data.PleosSdkSession
import com.example.travelcrumbs.data.gleo.GleoActionRepository
import com.example.travelcrumbs.ui.shell.AppShellScreen
import com.example.travelcrumbs.ui.theme.TravelCrumbsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 유일한 액티비티 — setContent 와 수명 · Intent 전달만. 화면은 앱 틀(AppShellScreen)이 주행 판정대로 고름.
 * Gleo Action 해석과 Callback 전송은 [GleoActionRepository] 가 함(여기서는 Intent 를 넘기기만).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sdkSession: PleosSdkSession

    @Inject
    lateinit var gleoActionRepository: GleoActionRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sdkSession.start()
        setContent {
            TravelCrumbsTheme {
                AppShellScreen()
            }
        }
        // 구성 변경·프로세스 복원 때 저장된 Intent를 업무 명령으로 다시 실행하지 않음.
        if (savedInstanceState == null) gleoActionRepository.handleActionIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        gleoActionRepository.handleActionIntent(intent)
    }

    override fun onDestroy() {
        // 화면 재생성(창 크기 변경 등) 동안 진행 중인 Gleo 장소 기록이 쓰는 SDK를 끊지 않음(#25).
        if (!isChangingConfigurations) sdkSession.stop()
        super.onDestroy()
    }
}
