# 환경 맞추기 — 맥(A) · 윈도우(B)

끝나면 `doctor.py`(맥 `python3 .claude/skills/sensors/scripts/doctor.py` · 윈도우 `python …`)가 FAIL 0. 빈 칸은 첫 이슈(#1)에서 builder 가 채움.

| 항목 | 맥 | 윈도우 |
|---|---|---|
| JDK | Android Studio 내장 JBR → `JAVA_HOME=/Applications/Android Studio.app/Contents/jbr/Contents/Home` | `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr` |
| Android SDK | `~/Library/Android/sdk` · 플랫폼 34 + compileSdk 플랫폼(`app/build.gradle.kts`) | `%LOCALAPPDATA%\Android\Sdk` · 같은 플랫폼 |
| Pleos 에뮬레이터 | SDK Manager → SDK Update Sites → Pleos 주소(**맥용 URL**) → SDK Platforms → API 34 Pleos Connect 이미지 | 같은 절차 · **윈도우/리눅스용 URL** (설치 가이드 https://document.pleos.ai/docs/connect/guide/getting-started/application-development/setup-connect-sdk-emulator) |
| AVD | Automotive · Pleos Connect v2.1.0(API 34) · RAM 4 GB · Cold boot · 이름 `Pleos_Connect_v2.1.0` | 같음 |
| Docker | Docker Desktop | Docker Desktop(WSL2 백엔드) |
| uv | `curl -LsSf https://astral.sh/uv/install.sh \| sh` → `uv python install 3.12` | `powershell -c "irm https://astral.sh/uv/install.ps1 \| iex"` → `uv python install 3.12` |
| gh | `brew install gh` → `gh auth login` | `winget install GitHub.cli` → `gh auth login` |
| Claude Code | 2.1.295 이상(훅의 `onFailure`) · **저장소 폴더에서 세션 시작**(다른 폴더에서 시작하면 `.claude/settings.json` 을 안 읽음) · 처음 열 때 폴더 신뢰(Trust) 한 번 → 훅(활동 로그 · guard) 켜짐 · `uv` 가 PATH 에 있어야 훅이 돎 | 같음 · Git Bash 가 없어도 됨(훅은 `uv` 를 직접 실행) |
| git 설정 | 기본값 | `git config --global core.autocrlf false` · `git config --global core.longpaths true` · 저장소는 `C:\dev\<repo>` 처럼 짧은 경로 |
| 서버 로컬 | `cd server && docker compose up -d db && uv sync && uv run alembic upgrade head && uv run uvicorn app.main:app --reload --port 4000` | 같은 명령(PowerShell) |
| 앱 → 서버 주소 | debug `http://10.0.2.2:4000` | 같음 |
| Virtual Console | `adb forward tcp:8088 tcp:8088` → http://127.0.0.1:8088/ | 같음 |
| Pleos 계정 연동(사람) | Pleos Playground 프로젝트 · Vehicle Data API 사용 신청(URL 3개) · `server/.env` 에 `PLEOS_CLIENT_ID` · `PLEOS_CLIENT_SECRET` · 승인 전엔 `PLEOS_AUTH_BASE_URL` 을 스텁(`http://127.0.0.1:4100`) | 같음 · 비밀은 저장소 밖으로 공유 |

첫 이슈 흐름: 이 표 → `doctor.py` 녹색 → 저장소 폴더에서 새 Claude Code 세션 → 폴더 신뢰 → 「이슈 #n 진행해줘」.

훅 확인(한 번): 세션에서 아무 명령이나 하나 실행 → `_workspace/_sessions/<오늘>.jsonl` 에 줄이 생기면 활동 로그 켜짐. 「`git push origin main --dry-run` 실행해줘」 → 권한 거부(deny 규칙) · 「`git -C . push origin main --dry-run` 실행해줘」 → `guard:` 로 막힘(훅) — 둘 다 막히면 권한 장치 켜짐.
