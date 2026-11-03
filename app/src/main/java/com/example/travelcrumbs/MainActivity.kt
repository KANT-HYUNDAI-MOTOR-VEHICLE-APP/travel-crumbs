package com.example.travelcrumbs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.travelcrumbs.data.PleosSdkSession
import com.example.travelcrumbs.ui.home.HomeScreen
import com.example.travelcrumbs.ui.theme.TravelCrumbsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 유일한 액티비티 — setContent 와 SDK 수명 이벤트 전달만.
 * Pleos SDK 3종의 initialize() · release() 는 [PleosSdkSession] 한 곳에서 한 번씩.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var sdkSession: PleosSdkSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sdkSession.start()
        setContent {
            TravelCrumbsTheme {
                HomeScreen()
            }
        }
    }

    override fun onDestroy() {
        sdkSession.stop()
        super.onDestroy()
    }
}
