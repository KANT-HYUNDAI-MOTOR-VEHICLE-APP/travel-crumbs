# <앱 이름>

Pleos Connect(Android Automotive) 차량 앱 + Python · FastAPI 서버를 두 사람 이상이 같은 Claude Code 하네스로 만드는 저장소. 지금은 **하네스와 협업 틀만 있는 시작 템플릿** — 앱(`app/`) · 서버(`server/`) 코드는 첫 이슈에서 생김.

- 진입점: `CLAUDE.md` · 하네스 안내: `.claude/README.md` · 협업 규칙: `team-workflow` 스킬 · 환경: `docs/setup.md`
- 결정 기록: `docs/architecture_decision_record/` — 0002~0011 은 하네스 원본 저장소에서 옮겨 온 근거(번호 0001 · 0004 는 원본의 제품 설계 문서라 뺌). 새 결정은 `0000-template.md` 꼴로 0012 부터.

## 처음 할 일(한 번 · 저장소 관리자)

PowerShell 5.1 은 `&&` 를 못 씀 — 명령은 한 줄씩.

### 1. 자리표시자 바꾸기

아래만 바꿈. 스킬 · 템플릿 안의 `<기능명>` · `<n>` · `<짧은이름>` · `<화면>` 같은 꺾쇠는 **일부러 남긴 칸**이라 그대로 둠.

| 자리표시자 | 바꿀 값 | 있는 곳 |
|---|---|---|
| `<앱 이름>` | 앱 이름(한글 그대로 가능) | `CLAUDE.md` · `README.md` · `.claude/` 에이전트 · 스킬 description · 문서 |
| `<owner>/<repo>` · `<repo>` | GitHub 저장소 주소 · 이름 | `team-workflow` · `.claude/log/README.md` · `docs/setup.md` · `test_guard.py` |
| `com.example.<패키지>` · `<앱>Application` | 앱 패키지 · Application 클래스 이름 | `android-architecture` |
| `<도메인>` · `<도메인 모델>` · `<도메인 테이블>` | 앱 · 서버의 실제 도메인 이름 — 첫 기능 스펙이 정해진 뒤 | `android-architecture` · `server-architecture` |
| 오케스트레이터 1단계 `[전제] <앱 이름> MVP — …` | 앱이 무엇을 하는지 한두 문장 · 주행 중 / 정차 중 동작 · 서버가 맡는 것 | `.claude/skills/app-orchestrator/SKILL.md` |

찾기:

```bash
git grep -n -e "<앱 이름>" -e "<owner>" -e "<repo>" -e "<패키지>" -e "<앱>" -e "<도메인" -e "\[전제\]"
```

바꾼 뒤 description 이 잘리지 않는지 확인(스킬 description 은 길이 한도가 있음):

```bash
python .claude/skills/sensors/scripts/check_skill_frontmatter.py
```

### 2. 저장소 만들고 develop 을 기본 브랜치로

하네스(guard · `settings.json` deny)는 main · develop 직접 푸시를 막음 — **첫 푸시는 Claude Code 밖, 사람 터미널에서.**

```bash
git init -b main
```

```bash
git add -A
```

```bash
git commit -m "chore: 하네스 시작 템플릿"
```

```bash
gh repo create <owner>/<repo> --private --source . --push
```

```bash
git switch -c develop
```

```bash
git push -u origin develop
```

```bash
gh repo edit --default-branch develop --enable-squash-merge --enable-merge-commit=false --enable-rebase-merge=false --delete-branch-on-merge
```

### 3. 브랜치 보호

- **공개 저장소 또는 GitHub Pro 이상**: `team-workflow` 8절의 `gh api … /branches/main/protection` 명령을 main · develop 에 한 번씩(`contexts` 는 CI 잡 이름 `app` · `server` · `common` — 아직 없는 잡은 그 잡이 생긴 뒤 넣음).
- **비공개 + 무료 플랜**: 보호 규칙 API 가 403 — 8절의 약속(직접 푸시 금지 · Squash 머지만 · 상대 승인 · 자기 PR 자기 머지 금지)을 두 사람이 지킴. guard 가 에이전트 쪽은 막음.

### 4. 환경 · doctor

`docs/setup.md` 표대로 맞춘 뒤 두 사람 모두:

```bash
python .claude/skills/sensors/scripts/doctor.py
```

FAIL 0 이 목표(앱 · 서버 항목 중 첫 이슈 전에는 없는 것은 WARN 일 수 있음). 윈도우는 `git config --global core.autocrlf false` · `git config --global core.longpaths true` 도.

### 5. Claude Code 와 첫 이슈

1. 저장소 폴더에서 Claude Code 세션 시작 → 폴더 신뢰(Trust) 한 번 → 훅(guard · 활동 로그) 켜짐. `uv` 가 PATH 에 있어야 함.
2. `harness` 플러그인(`harness:evolve` · `harness:harness`)을 두 사람 모두 설치 — 하네스 피드백 흐름이 씀.
3. GitHub 에서 `[기능]` 이슈 #1 「환경 · 빌드 · CI 녹색」을 만들고 담당자 지정 → 세션에 「이슈 #1 진행해줘」. 할 일은 `app-orchestrator` 「첫 이슈 권장」 절(Android 프로젝트 · Gradle · 서버 뼈대 · CI `app` 잡 · `docs/setup.md` 빈칸).
4. 그 뒤로 모든 작업은 이슈로 시작 — 이슈 번호 없는 구현 요청은 하네스가 이슈 만들기부터 도움(`CLAUDE.md` 「이슈 게이트」).

## 템플릿에서 뺀 것

원본 제품의 `app/` · `server/` · Gradle 루트 파일 · 기능 스펙(`docs/spec/<기능>/`) · API 계약(`docs/api/openapi.json`) · 이슈별 작업 기록(`.claude/log/<n>-*`) · `_workspace/` · `.idea/`. 권한 허용 목록(`check_manifest.py` 의 `ALLOWED`)은 기능에 묶인 권한(위치 · 포그라운드 서비스)을 뺀 최소 목록 — 늘리는 길은 `sensors` 스킬 「권한 허용 목록을 늘리는 길」.
