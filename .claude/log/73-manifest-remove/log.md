# #73 [하네스 피드백] check_manifest 가 tools:node="remove" 권한 지우기 선언을 요청 권한으로 셈

- 이슈: https://github.com/KANT-HYUNDAI-MOTOR-VEHICLE-APP/travel-crumbs/issues/73
- 담당: hiju200208-png
- 브랜치: harness/73-manifest-remove → develop

## 반영 끝 (2026-11-10)
- 계기: #59 PR #71 에서 MapLibre 가 끌어온 위치 · Wi-Fi 권한을 tools:node="remove" 로 지웠는데 센서가 위반 3 으로 셈(심사관 리뷰 · release 병합 매니페스트엔 없음)
- 바꾼 것: check_manifest.py 가 remove · removeAll 선언을 요청 권한에서 빼고 「제거 선언 n개」로 알림 · merge 등은 그대로 셈 · ALLOWED 변경 없음
- 시험: test_check_manifest.py RemovedByMerge 4건(remove 통과 · remove 없는 같은 권한 위반 · merge 위반 · 지운 백그라운드 위치 통과) · 전체 11건 통과
- 문서: sensors 스킬 「권한 허용 목록을 늘리는 길」 한 단락 · ADR 0017
- 확인: develop 매니페스트 위반 0(변화 없음) · PR #71 매니페스트 위반 0(제거 선언 3개)
- 에이전트: 메인 직접(harness:evolve 스킬이 이 환경 목록에 없어 하네스 흐름 절차대로 직접 반영)
- 다음: develop PR · 상대 승인 · 머지 뒤 PR #71 이 develop 받으면 CI 녹색
