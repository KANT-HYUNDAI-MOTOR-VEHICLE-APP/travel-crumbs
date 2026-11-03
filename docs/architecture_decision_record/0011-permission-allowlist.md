# ADR 0011 — 권한 허용 목록의 원본과 늘리는 기준(2026-11-02 제안)

> 템플릿 주: 하네스 원본 저장소에서 내린 결정을 근거로 옮겨 온 기록. 본문의 이슈 · PR 번호와 날짜는 원본 저장소 것이며, 이 저장소의 새 결정은 다음 번호부터 `0000-template.md` 꼴로 씀.

> 원본 규칙: `sensors` 스킬 「권한 허용 목록을 늘리는 길」 · 목록 자체는 `.claude/skills/sensors/scripts/check_manifest.py` 의 `ALLOWED`. 원본 저장소의 하네스 피드백 이슈(발단은 한 기능 스펙).

## 무엇이 있었나

한 기능 스펙이 오디오 재생 유지를 포그라운드 서비스로 정하면서 `FOREGROUND_SERVICE` · `FOREGROUND_SERVICE_MEDIA_PLAYBACK` 이 필요해졌는데, 센서 `check_manifest.py` 의 허용 목록에 없어 구현하면 빨강이 됨. `--allow` 로 넘길 수는 있으나 그러면 로컬과 CI 명령이 달라짐(`sensors` 의 전제는 「명령은 로컬과 CI 가 같다」).

더 큰 문제는 **같은 허용 목록이 세 곳에 베껴져 있던 것** — `check_manifest.py` 의 `ALLOWED` · `pleos-review-checklist` 의 심사 질문 · `pleos-vehicle-sdk` 2절. 한 곳만 고치면 규칙과 센서가 어긋남. 그리고 **무엇을 근거로 목록에 넣어도 되는지 기준이 어디에도 없었음.**

## 결정

- **원본은 한 곳** — `check_manifest.py` 의 `ALLOWED`. set 이던 것을 **권한 → 왜 쓰는지** 의 표로 바꿔 근거를 코드 옆에 둠. 다른 문서는 목록을 베끼지 않고 이 스크립트를 가리킴.
- **늘리는 기준 넷**(`sensors` 「권한 허용 목록을 늘리는 길」) — ① 승인된 스펙에 왜 필요한지 적혀 있음 ② 권한 없이 할 길이 없음 ③ 심사 가이드에 어긋나지 않음 ④ `ALLOWED` 에 근거를 함께 적음. 하나라도 비면 넣지 않음.
- **목록 변경은 하네스 변경** — `[하네스 피드백]` 이슈 → `harness:evolve`. 기능 브랜치에서 슬쩍 늘리지 않음.
- **`--allow` 는 스펙 승인 전 손으로 한 번 확인할 때만** — 커밋하는 명령 · CI 잡에는 쓰지 않음.
- 원본 저장소는 그 기준에 따라 `FOREGROUND_SERVICE` · `FOREGROUND_SERVICE_MEDIA_PLAYBACK` 을 근거와 함께 넣음. 이 템플릿의 `ALLOWED` 는 기능에 묶인 권한을 뺀 최소 목록에서 시작함.
- 쓰지 않게 된 권한은 그 기능을 지우는 변경에서 함께 뺌.

## 이유

| 무엇 | 이유 |
|---|---|
| 목록을 한 곳에만 | 세 곳에 있으면 한 곳만 고쳐져 「규칙은 금지인데 센서는 통과」 같은 어긋남이 남음. 이번에 실제로 그 상태였음 |
| 근거를 코드 옆에 | 근거 없는 줄은 다음 사람이 지울 수도 남길 수도 없게 됨. 권한은 늘기만 하고 줄지 않는 쪽으로 썩음 |
| 스펙을 근거로 | 권한은 기능에서 나옴. 코드를 쓰다 「필요해서」 늘리면 심사 「최소 권한」이 무너짐 |
| `--allow` 를 CI 에서 금지 | 한쪽에만 붙으면 「명령은 로컬과 CI 가 같다」가 깨지고, 로컬 녹색이 CI 녹색을 뜻하지 않게 됨 |
| 금지 권한은 예외 없이 | 백그라운드 위치는 `--allow` 로도 통과하지 못하게 둠(시험으로 고정) |

## 곁다리로 고친 것 — 윈도우에서 센서가 터지던 문제

`doctor.py` · `check_repo_hygiene.py` · `check_secrets.py` · `trace_report.py` · `guard.py` 등 한글을 출력하는 스크립트가 **윈도우 기본 콘솔 인코딩(cp1252 · cp949)에서 `UnicodeEncodeError` 로 죽고 있었음.** `PYTHONIOENCODING=utf-8` 을 앞에 붙여야만 돌았는데, 그러면 두 OS 의 명령이 달라져 이 이슈의 완료 조건(「로컬과 CI 가 같은 명령」)과 정면으로 부딪침.

- 고침: 한글을 내보내는 스크립트 11개에 `sys.stdout/stderr.reconfigure(encoding="utf-8")` 한 줄씩. 시험 둘은 `subprocess` 가 읽을 때도 UTF-8 로 디코딩하게 `encoding="utf-8"` 추가.
- 결과: `guardrails` 시험이 **20 통과 · 106 subtest 통과**가 됨. 전에 실패하던 `test_trace::test_report` 가 바로 이 문제였음(출력 디코딩 실패로 `stdout` 이 `None`).

## 기계로 막는 것

| 무엇 | 어떻게 |
|---|---|
| 근거 없는 허용 줄 | `test_check_manifest.py::Allowlist::test_every_entry_has_a_reason` |
| 허용 밖 권한 · 금지 권한(`--allow` 로도) | 같은 파일 `Blocked` |
| 새 권한 둘이 실제로 통과하는가 | 같은 파일 `test_foreground_service_is_allowed` |
| CI 에서 빠져 있던 센서 | `ci.yml` `common` 잡에 `test_check_manifest.py` · `check_skill_frontmatter.py` 추가(후자는 ADR 0010 때 문서에만 적고 `ci.yml` 이 저장소에 없어 못 넣었던 것) |
| 「목록을 베끼지 않음」 | 약속(기계로 재지 못함). 다만 베낄 목록이 문서에서 사라져 베끼기 어려워짐 |

## 남은 것

- **CI 에 app 잡이 없음** — `ci.yml` 에 `changes` · `server` · `common` 만 있고 앱 Lint · 단위 테스트 · Konsist · `assembleRelease` · `check_manifest` 가 없음. 그래서 매니페스트 센서는 지금 로컬에서만 돎. 이 ADR 의 범위 밖이라 따로 이슈를 둘 것.
