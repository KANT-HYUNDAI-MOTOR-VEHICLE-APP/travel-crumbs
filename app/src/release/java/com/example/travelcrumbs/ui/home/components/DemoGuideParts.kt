package com.example.travelcrumbs.ui.home.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/*
 * release 자리 — 아무것도 그리지 않음(스펙 navigation 2.3 G1-D · G3 · G5 · 결정 기록 U55 · U70 · 심사 A1).
 * 실제 부품 · 문구는 debug 소스 세트에만 있음. 화면 상태도 release 에선 늘 꺼짐(DemoFeatures · BuildConfig.DEBUG).
 */

@Suppress("UNUSED_PARAMETER")
@Composable
fun DummyRouteButton(enabled: Boolean, onClick: () -> Unit) = Unit

@Suppress("UNUSED_PARAMETER")
@Composable
fun DummyRouteNotice(modifier: Modifier = Modifier) = Unit

@Composable
fun FallbackLabel() = Unit
